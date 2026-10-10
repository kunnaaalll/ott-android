import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { getSignedCookies } from "@aws-sdk/cloudfront-signer";

// AWS CloudFront configuration — loaded from Firebase Secret Manager at runtime.
// These MUST be set via: firebase functions:secrets:set CLOUDFRONT_KEY_PAIR_ID etc.
// They must NEVER be committed to source control.


// Rate-limiting: maximum authorizePlayback calls per user within the window.
const RATE_LIMIT_MAX_CALLS = 20;
const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute

/**
 * Enforces a per-user rate limit stored in Firestore.
 * Throws RESOURCE_EXHAUSTED if the user has exceeded the call quota.
 */
async function enforceRateLimit(db: admin.firestore.Firestore, userId: string): Promise<void> {
    const now = Date.now();
    const windowStart = now - RATE_LIMIT_WINDOW_MS;
    const rateLimitRef = db.collection("_rateLimits").doc(`playback:${userId}`);

    await db.runTransaction(async (transaction) => {
        const doc = await transaction.get(rateLimitRef);
        const data = doc.exists ? doc.data()! : { calls: [] as number[] };

        // Prune calls outside the window
        const recentCalls: number[] = (data.calls as number[]).filter((t: number) => t > windowStart);

        if (recentCalls.length >= RATE_LIMIT_MAX_CALLS) {
            throw new functions.https.HttpsError(
                "resource-exhausted",
                "Too many authorization requests. Please try again later."
            );
        }

        recentCalls.push(now);
        transaction.set(rateLimitRef, { calls: recentCalls }, { merge: false });
    });
}

export const authorizePlaybackHandler = async (data: any, context: any) => {
    // 1. Require authenticated caller — UID comes from verified Firebase ID token.
    if (!context.auth || !context.auth.uid) {
        throw new functions.https.HttpsError("unauthenticated", "UNAUTHENTICATED");
    }
    const userId = context.auth.uid;

    // 2. Validate request parameters — all trusted decisions made server-side.
    const contentId = data.contentId;
    const isTrailer = data.isTrailer === true;

    if (!contentId || typeof contentId !== "string" || contentId.length === 0 || contentId.length > 128) {
        throw new functions.https.HttpsError("invalid-argument", "Missing or invalid contentId");
    }

    // Guard against path-traversal injected in the contentId parameter itself.
    if (contentId.includes("/") || contentId.includes("..") || contentId.includes("\\")) {
        throw new functions.https.HttpsError("invalid-argument", "Invalid contentId format");
    }

    const db = admin.firestore();

    // 3. Rate-limit before doing any expensive Firestore reads.
    await enforceRateLimit(db, userId);

    // 4. Load and validate the content document.
    const contentDoc = await db.collection("content").doc(contentId).get();
    if (!contentDoc.exists) {
        // Deliberately generic: do not reveal whether the content ID exists to unauthorized callers.
        throw new functions.https.HttpsError("not-found", "CONTENT_NOT_FOUND");
    }

    const contentData = contentDoc.data()!;
    if (contentData.status !== "PUBLISHED") {
        // Do not reveal draft/unpublished status to clients.
        throw new functions.https.HttpsError("failed-precondition", "CONTENT_NOT_AVAILABLE");
    }

    // Derive trusted tenantId from the content document — NOT from the client request.
    const tenantId: string | undefined = contentData.tenantId;
    if (!tenantId || typeof tenantId !== "string") {
        functions.logger.error("Content document missing tenantId", { contentId });
        throw new functions.https.HttpsError("internal", "INTERNAL_ERROR");
    }

    // 5. Cross-tenant isolation: verify the authenticated user belongs to the same tenant.
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
        // Could happen if user record creation is async; treat as unauthenticated.
        throw new functions.https.HttpsError("unauthenticated", "USER_NOT_FOUND");
    }
    const userData = userDoc.data()!;
    if (userData.tenantId !== tenantId) {
        // Log the attempted cross-tenant access for monitoring.
        functions.logger.warn("Cross-tenant playback attempt", {
            userId,
            userTenantId: userData.tenantId,
            contentTenantId: tenantId,
            contentId,
            isTrailer,
        });
        throw new functions.https.HttpsError("permission-denied", "CROSS_TENANT_ACCESS_DENIED");
    }

    // 6. Subscription check — only required for main content, not trailers.
    if (!isTrailer) {
        const subscriptionsSnapshot = await db
            .collection("subscriptions")
            .where("userId", "==", userId)
            .where("tenantId", "==", tenantId)
            .where("status", "==", "ACTIVE")
            .get();

        if (subscriptionsSnapshot.empty) {
            throw new functions.https.HttpsError("permission-denied", "SUBSCRIPTION_REQUIRED");
        }

        // Validate expiry server-side using server clock — never trust the client.
        const now = admin.firestore.Timestamp.now();
        const hasValidSubscription = subscriptionsSnapshot.docs.some((doc) => {
            const subData = doc.data();
            return subData.expiryDate && subData.expiryDate.toMillis() > now.toMillis();
        });

        if (!hasValidSubscription) {
            // Return the same generic error code — client should not distinguish
            // "no subscription" from "expired subscription" for security hardening.
            throw new functions.https.HttpsError("permission-denied", "SUBSCRIPTION_REQUIRED");
        }
    }

    // 7. Load the appropriate playback asset from trusted Firestore data.
    //    The client NEVER provides the asset path — it is derived entirely server-side.
    const asset = isTrailer ? contentData.trailerPlaybackAsset : contentData.mainPlaybackAsset;
    const hlsPath: string | undefined = asset?.hlsPath;

    if (!hlsPath || typeof hlsPath !== "string") {
        throw new functions.https.HttpsError("failed-precondition", "CONTENT_NOT_AVAILABLE");
    }

    // 8. Path validation — enforce strict prefix and traversal guards.
    //    This protects against corrupted Firestore data or insider-written records.
    const assetDir = isTrailer ? "trailer" : "main";
    const expectedPrefix = `content/${tenantId}/${contentId}/${assetDir}/hls/`;

    if (!hlsPath.startsWith(expectedPrefix)) {
        functions.logger.error("HLS path does not match expected prefix", {
            hlsPath,
            expectedPrefix,
            contentId,
            tenantId,
            isTrailer,
        });
        throw new functions.https.HttpsError("permission-denied", "INVALID_ASSET_PATH");
    }

    // Reject any path traversal sequences that could have been stored in Firestore.
    if (hlsPath.includes("../") || hlsPath.includes("..\\") || hlsPath.includes("%2e%2e")) {
        functions.logger.error("Path traversal detected in hlsPath", { hlsPath, contentId });
        throw new functions.https.HttpsError("permission-denied", "INVALID_ASSET_PATH");
    }

    // Reject unexpected characters that could be used for injection.
    if (!/^[a-zA-Z0-9/_.\-]+$/.test(hlsPath)) {
        functions.logger.error("Unexpected characters in hlsPath", { contentId });
        throw new functions.https.HttpsError("permission-denied", "INVALID_ASSET_PATH");
    }

    // 9. Verify CloudFront infrastructure is configured before attempting to sign.
    const CLOUDFRONT_KEY_PAIR_ID = process.env.CLOUDFRONT_KEY_PAIR_ID;
    const CLOUDFRONT_PRIVATE_KEY = process.env.CLOUDFRONT_PRIVATE_KEY;
    const CLOUDFRONT_DOMAIN = process.env.CLOUDFRONT_DOMAIN;

    if (!CLOUDFRONT_KEY_PAIR_ID || !CLOUDFRONT_PRIVATE_KEY || !CLOUDFRONT_DOMAIN) {
        // Fail explicitly — do not fabricate a successful playback URL.
        functions.logger.error("CloudFront playback infrastructure is not configured.");
        throw new functions.https.HttpsError("failed-precondition", "PLAYBACK_NOT_CONFIGURED");
    }

    // 10. Generate a narrowly-scoped CloudFront signed-cookie policy.
    //     The resource URL covers only this content item's asset directory —
    //     NOT the entire distribution or content root.
    //
    //     MAIN:    https://{domain}/content/{tenantId}/{contentId}/main/hls/*
    //     TRAILER: https://{domain}/content/{tenantId}/{contentId}/trailer/hls/*
    //
    //     A trailer cookie cannot access main content, and vice versa.
    //     One content item's cookie cannot access another content item's assets.
    const resourceUrl = `https://${CLOUDFRONT_DOMAIN}/${expectedPrefix}*`;

    // 15-minute expiration — short-lived; calculated server-side.
    const expirationMs = Date.now() + 1000 * 60 * 15;
    const dateLessThan = new Date(expirationMs).toISOString();

    // Normalize PEM line endings (Firebase Secrets stores multi-line values with \n literals).
    const privateKey = CLOUDFRONT_PRIVATE_KEY.replace(/\\n/g, "\n");

    let signedCookies: any;
    try {
        signedCookies = getSignedCookies({
            keyPairId: CLOUDFRONT_KEY_PAIR_ID,
            privateKey: privateKey,
            url: resourceUrl,
            dateLessThan: dateLessThan,
        });
    } catch (signingError) {
        // Log without revealing key material.
        functions.logger.error("CloudFront cookie signing failed", {
            contentId,
            isTrailer,
            errorType: (signingError as Error).constructor?.name,
        });
        throw new functions.https.HttpsError("internal", "SIGNING_ERROR");
    }

    // 11. Structured authorization audit log — no secrets, no cookie values.
    functions.logger.info("Playback authorized", {
        userId,
        contentId,
        tenantId,
        isTrailer,
        assetDir,
        expiresAt: expirationMs,
    });

    // 12. Return the signed cookies and playback URL.
    //     Cookie VALUES are included — the client needs them to attach to HLS requests.
    //     The private key is NEVER included in the response.
    return {
        authorized: true,
        playbackUrl: `https://${CLOUDFRONT_DOMAIN}/${hlsPath}`,
        expiresAt: expirationMs,
        cookies: {
            "CloudFront-Policy": signedCookies["CloudFront-Policy"],
            "CloudFront-Signature": signedCookies["CloudFront-Signature"],
            "CloudFront-Key-Pair-Id": signedCookies["CloudFront-Key-Pair-Id"],
        },
    };
};

export const authorizePlayback = functions.https.onCall(authorizePlaybackHandler);
