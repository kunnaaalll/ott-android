import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

admin.initializeApp();
const db = admin.firestore();

// GATEWAY_WEBHOOK_SECRET must be configured via: firebase functions:secrets:set GATEWAY_WEBHOOK_SECRET
// If not set, all webhook requests will be rejected (fail-safe).
const WEBHOOK_SECRET = process.env.GATEWAY_WEBHOOK_SECRET;

/**
 * Validates the webhook signature based on the gateway's signing mechanism.
 */
/**
 * Validates the webhook signature using HMAC-SHA256.
 *
 * DEPLOYMENT REQUIREMENT:
 *   The GATEWAY_WEBHOOK_SECRET must be set via Firebase Secret Manager:
 *     firebase functions:secrets:set GATEWAY_WEBHOOK_SECRET
 *   Bind it to paymentWebhook in firebase.json:
 *     "secretEnvironmentVariables": [{ "key": "GATEWAY_WEBHOOK_SECRET" }]
 *
 * The exact header name and signing algorithm must match your payment gateway's
 * documentation (e.g., Stripe uses "Stripe-Signature" with a timestamp prefix;
 * Razorpay uses "x-razorpay-signature" with raw-body HMAC).
 *
 * Update the header name and hashing logic below to match your gateway.
 */
function verifySignature(req: functions.https.Request): boolean {
    if (!WEBHOOK_SECRET) {
        // Fail-safe: reject all requests if the secret is not configured.
        functions.logger.error("GATEWAY_WEBHOOK_SECRET is not configured. All webhook requests are rejected.");
        return false;
    }

    // TODO: Replace "x-gateway-signature" with your gateway's actual header name.
    const signature = req.headers["x-gateway-signature"] as string;
    if (!signature) {
        functions.logger.warn("Webhook request missing signature header.");
        return false;
    }

    // HMAC-SHA256 of the raw request body against the shared secret.
    // req.rawBody is populated by Firebase Functions for HTTP triggers.
    if (!req.rawBody) {
        functions.logger.error("rawBody not available for webhook signature verification.");
        return false;
    }

    const expectedSignature = crypto
        .createHmac("sha256", WEBHOOK_SECRET)
        .update(req.rawBody)
        .digest("hex");

    // Use timing-safe comparison to prevent timing attacks.
    try {
        return crypto.timingSafeEqual(
            Buffer.from(signature, "hex"),
            Buffer.from(expectedSignature, "hex")
        );
    } catch {
        // timingSafeEqual throws if buffers have different lengths.
        return false;
    }
}

export const paymentWebhook = functions.https.onRequest(async (req, res) => {
    try {
        if (!verifySignature(req)) {
            res.status(401).send("Invalid or unconfigured signature");
            return;
        }

        const event = req.body;
        // Every event from the gateway should have a unique ID
        const eventId = event.id;
        
        if (!eventId) {
            res.status(400).send("Missing event ID");
            return;
        }

        // Idempotency check:
        // Use Firestore transactions to ensure an event is processed exactly once
        const eventRef = db.collection("webhook_events").doc(eventId);
        
        await db.runTransaction(async (transaction) => {
            const eventDoc = await transaction.get(eventRef);
            
            if (eventDoc.exists) {
                // Already processed this event, ignore safely
                functions.logger.info(`Event ${eventId} already processed.`);
                return;
            }

            // Extract relevant information based on event type
            const eventType = event.type;
            const gatewaySubscriptionId = event.data?.subscription_id;
            const userId = event.data?.metadata?.userId;
            const tenantId = event.data?.metadata?.tenantId;
            const planId = event.data?.metadata?.planId;
            
            if (!userId || !tenantId || !planId) {
                functions.logger.error("Missing metadata for subscription assignment.");
                // Mark event as processed but failed to apply
                transaction.set(eventRef, { processedAt: admin.firestore.FieldValue.serverTimestamp(), error: "Missing metadata" });
                return;
            }

            // Unknown User Check: Ensure the user actually exists before assigning a subscription.
            const userDoc = await transaction.get(db.collection("users").doc(userId));
            if (!userDoc.exists) {
                functions.logger.error(`Unknown user: ${userId}. Cannot assign subscription.`);
                transaction.set(eventRef, { processedAt: admin.firestore.FieldValue.serverTimestamp(), error: "Unknown user" });
                return;
            }

            // Cross-tenant isolation: verify the tenantId in the webhook metadata
            // actually matches the authenticated user's tenant in our database.
            // This prevents a crafted webhook from injecting a subscription into
            // a different tenant's user account.
            const userTenantId = userDoc.data()?.tenantId;
            if (!userTenantId || userTenantId !== tenantId) {
                functions.logger.error(`Tenant mismatch for user ${userId}. Event tenantId: ${tenantId}, user tenantId: ${userTenantId}`);
                transaction.set(eventRef, { processedAt: admin.firestore.FieldValue.serverTimestamp(), error: "Tenant mismatch" });
                return;
            }

            // Find or create the subscription document
            const subscriptionsQuery = await db.collection("subscriptions")
                .where("userId", "==", userId)
                .where("tenantId", "==", tenantId)
                .limit(1)
                .get();

            let subscriptionRef;
            if (!subscriptionsQuery.empty) {
                subscriptionRef = subscriptionsQuery.docs[0].ref;
            } else {
                subscriptionRef = db.collection("subscriptions").doc();
            }

            // Handle the business logic based on event type
            switch (eventType) {
                case "subscription.activated":
                case "payment.success": {
                    // Update subscription as ACTIVE with new expiry date
                    const expiryDate = new Date();
                    expiryDate.setMonth(expiryDate.getMonth() + 1); // E.g., add 1 month
                    
                    transaction.set(subscriptionRef, {
                        userId,
                        tenantId,
                        planId,
                        gatewaySubscriptionId: gatewaySubscriptionId || "",
                        status: "ACTIVE",
                        startDate: admin.firestore.FieldValue.serverTimestamp(),
                        expiryDate: admin.firestore.Timestamp.fromDate(expiryDate),
                        updatedAt: admin.firestore.FieldValue.serverTimestamp()
                    }, { merge: true });
                    break;
                }
                case "payment.failed": {
                    transaction.set(subscriptionRef, {
                        userId,
                        tenantId,
                        planId,
                        status: "FAILED",
                        updatedAt: admin.firestore.FieldValue.serverTimestamp()
                    }, { merge: true });
                    break;
                }
                case "subscription.cancelled": {
                    // Depends on gateway: Does cancellation mean immediate revoke or no-renew?
                    // Assuming we mark as CANCELLED, but expiryDate remains intact
                    transaction.set(subscriptionRef, {
                        status: "CANCELLED",
                        updatedAt: admin.firestore.FieldValue.serverTimestamp()
                    }, { merge: true });
                    break;
                }
                default: {
                    functions.logger.info(`Unhandled event type: ${eventType}`);
                }
            }

            // Mark event as processed
            transaction.set(eventRef, {
                processedAt: admin.firestore.FieldValue.serverTimestamp(),
                type: eventType,
                gatewaySubscriptionId
            });
        });

        res.status(200).send("Webhook received");
    } catch (error) {
        functions.logger.error("Error processing webhook:", error);
        res.status(500).send("Internal Server Error");
    }
});

export { authorizePlayback } from "./playback";
export { createMediaUpload, completeMediaUpload, mediaConvertWebhook, changeContentStatus } from "./media";

