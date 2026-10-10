import * as admin from "firebase-admin";
import { authorizePlaybackHandler } from "./playback";

// Ensure tests point to the Firestore emulator
process.env.FIRESTORE_EMULATOR_HOST = "localhost:8080";
process.env.FIREBASE_AUTH_EMULATOR_HOST = "localhost:9099";
process.env.GCLOUD_PROJECT = "demo-ott-staging"; // Dummy project ID for emulator

describe("Playback Rate Limiter - Concurrent Integration Test", () => {
    let db: admin.firestore.Firestore;

    beforeAll(async () => {
        // Ensure emulator is running to prevent ambiguous test timeouts
        try {
            await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}`);
        } catch (error) {
            throw new Error("Firestore emulator is not running! Start it with 'npm run serve' before running integration tests.");
        }

        // Initialize the admin SDK if not already initialized
        if (!admin.apps.length) {
            admin.initializeApp({ projectId: "demo-ott-staging" });
        }
        db = admin.firestore();
    });

    afterEach(async () => {
        if (!db) return;
        // Clean up the _rateLimits collection between tests to ensure isolation
        const snapshot = await db.collection("_rateLimits").get();
        const batch = db.batch();
        snapshot.docs.forEach((doc) => {
            batch.delete(doc.ref);
        });
        await batch.commit();
    });

    it("enforces a maximum of 20 calls per user under concurrent execution", async () => {
        const userId = "test-user-1";
        const contentId = "content-123";
        const tenantId = "tenant-A";

        // Seed necessary content and user data for authorization
        await db.collection("users").doc(userId).set({ tenantId });
        await db.collection("content").doc(contentId).set({
            status: "PUBLISHED",
            tenantId,
            mainPlaybackAsset: { hlsPath: `content/${tenantId}/${contentId}/main/hls/master.m3u8` }
        });
        await db.collection("subscriptions").add({
            userId,
            tenantId,
            status: "ACTIVE",
            expiryDate: admin.firestore.Timestamp.fromMillis(Date.now() + 1000 * 60 * 60 * 24) // 1 day
        });

        // Set dummy AWS credentials to pass configuration checks
        process.env.CLOUDFRONT_KEY_PAIR_ID = "dummy-key-id";
        process.env.CLOUDFRONT_PRIVATE_KEY = "dummy-private-key";
        process.env.CLOUDFRONT_DOMAIN = "dummy.cloudfront.net";

        const reqData = { contentId };
        const reqContext = { auth: { uid: userId } };

        // Fire 20 concurrent requests (should all succeed)
        const successfulRequests = Array.from({ length: 20 }).map(() =>
            authorizePlaybackHandler(reqData as any, reqContext as any)
        );

        await expect(Promise.all(successfulRequests)).resolves.toBeDefined();

        // The 21st request should be rate limited
        await expect(
            authorizePlaybackHandler(reqData as any, reqContext as any)
        ).rejects.toThrow("Too many authorization requests. Please try again later.");
    });

    it("does not incorrectly share rate limit counters between independent users", async () => {
        const userId1 = "test-user-A";
        const userId2 = "test-user-B";
        const contentId = "content-123";
        const tenantId = "tenant-A";

        // Seed data for both users
        await db.collection("users").doc(userId1).set({ tenantId });
        await db.collection("users").doc(userId2).set({ tenantId });
        await db.collection("content").doc(contentId).set({
            status: "PUBLISHED",
            tenantId,
            mainPlaybackAsset: { hlsPath: `content/${tenantId}/${contentId}/main/hls/master.m3u8` }
        });
        await db.collection("subscriptions").add({ userId: userId1, tenantId, status: "ACTIVE", expiryDate: admin.firestore.Timestamp.fromMillis(Date.now() + 1000 * 60 * 60 * 24) });
        await db.collection("subscriptions").add({ userId: userId2, tenantId, status: "ACTIVE", expiryDate: admin.firestore.Timestamp.fromMillis(Date.now() + 1000 * 60 * 60 * 24) });

        process.env.CLOUDFRONT_KEY_PAIR_ID = "dummy";
        process.env.CLOUDFRONT_PRIVATE_KEY = "dummy";
        process.env.CLOUDFRONT_DOMAIN = "dummy.cloudfront.net";

        // Exhaust limit for User 1
        for (let i = 0; i < 20; i++) {
            await authorizePlaybackHandler({ contentId } as any, { auth: { uid: userId1 } } as any);
        }

        await expect(authorizePlaybackHandler({ contentId } as any, { auth: { uid: userId1 } } as any))
            .rejects.toThrow("Too many authorization requests. Please try again later.");

        // User 2 should still be able to authorize
        await expect(authorizePlaybackHandler({ contentId } as any, { auth: { uid: userId2 } } as any))
            .resolves.toBeDefined();
    });
});
