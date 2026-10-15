# Unified OTT Platform — Unified Logical API Contract

- **Document Version:** 1.0.0
- **Document Status:** PROPOSED LOGICAL SPECIFICATION (Phase 2 Deliverable)
- **Author:** Principal Backend Architect, Application Security Architect
- **Target Audience:** Android Native Engineers, Web Frontend Engineers, Backend Engineers
- **Communication Protocol:** Firebase Cloud Functions (v1/v2 HTTPS Callables & HTTP Webhook Triggers) and Firestore Client SDK (secured by Declarative Rules v2).

---

## 1. API Architecture & Calling Conventions

The unified OTT platform employs a hybrid communication model:
1. **Direct Firestore Document Access (Client SDK):** Used for low-latency, read-heavy operations (catalog browsing, category listings, subscription plans, user watchlists) where access control is declaratively enforced by `firestore.rules`.
2. **Cloud Functions Callables (`functions.https.onCall`):** Used for security-sensitive operations requiring server-side orchestration, credential signing, or rate-limiting (e.g., playback authorization, media upload pre-signing, status transitions). Callables automatically validate Firebase ID tokens and pass authenticated context (`context.auth.uid`).
3. **HTTP Webhooks (`functions.https.onRequest`):** Used for unauthenticated third-party asynchronous event ingestion (Payment Gateway webhooks, AWS MediaConvert SNS callbacks). These endpoints enforce cryptographic signature verification over raw request buffers.

### 1.1 Standard Error Codes & Envelope

All Firebase Callable operations return structured exceptions adhering to standard `HttpsError`:

| Code | HTTP Status | Description |
|---|---|---|
| `unauthenticated` | 401 | Missing, expired, or invalid Firebase ID token. |
| `permission-denied` | 403 | Authenticated caller lacks permissions (e.g. non-admin, cross-tenant access, or missing subscription). |
| `invalid-argument` | 400 | Missing or malformed parameters; validation constraint violated. |
| `not-found` | 404 | Target entity does not exist. |
| `failed-precondition` | 412 | System state does not permit operation (e.g. unready video cannot be published, CloudFront unconfigured). |
| `resource-exhausted` | 429 | Per-user or per-IP rate limit exceeded. |
| `internal` | 500 | Unhandled internal cloud or signing failure. |

---

## 2. Authentication & User Profile Domain

### 2.1 `AUTH-01: Get Current User Profile`
- **Mechanism:** Direct Firestore Document Read via Client SDK
- **Path:** `/users/{userId}`
- **Calling Clients:** Android App, Consumer Web, Admin CMS
- **Authentication:** Required (`request.auth.uid == userId`)
- **Authorization:** Owner only (enforced by `firestore.rules:37`)
- **Tenant Scope:** Scoped to user's assigned `tenantId`
- **Request:** None (document ID is authenticated UID)
- **Response Schema:**
```json
{
  "id": "usr_98a7sd8f7a",
  "email": "subscriber@example.com",
  "name": "Alex Johnson",
  "photoUrl": "https://cdn.example.com/profiles/alex.jpg",
  "tenantId": "tenant_sundari",
  "isAdmin": false,
  "createdAt": "2026-10-15T12:00:00Z",
  "lastLoginAt": "2026-10-16T00:00:00Z"
}
```
- **Error Behavior:** Missing document returns null/not-found.
- **Implementation Status:** **IMPLEMENTED** (Verified in Android `UserRepositoryImpl.kt` and `firestore.rules`).

---

### 2.2 `AUTH-02: Register User Profile Initializer`
- **Mechanism:** Firebase Callable (`assignUserTenant`) or Document Create with Server Validation
- **Path:** `assignUserTenant` / Create `/users/{userId}`
- **Calling Clients:** Android App, Consumer Web
- **Authentication:** Required (`request.auth.uid == userId`)
- **Authorization:** `firestore.rules:42` prohibits client from self-assigning `isAdmin` or `tenantId`.
- **Tenant Scope:** Derived from client application build (`BuildConfig.TENANT_ID`) or Web request domain.
- **Request Schema:**
```json
{
  "name": "Alex Johnson",
  "email": "subscriber@example.com"
}
```
- **Response Schema:**
```json
{
  "success": true,
  "tenantId": "tenant_sundari",
  "userId": "usr_98a7sd8f7a"
}
```
- **Idempotency:** Safe to retry; merges fields if already created.
- **Implementation Status:** **PARTIAL** (Android writes profile directly; dedicated server-authoritative tenant assignment function to be formalized in Phase 3).

---

## 3. Catalog & Discovery Domain

### 3.1 `CAT-01: List Published Content`
- **Mechanism:** Direct Firestore Query via Client SDK
- **Path:** `/content` where `tenantId == :tenantId` and `status == "PUBLISHED"`
- **Calling Clients:** Android App, Consumer Web
- **Authentication:** Required (`isSignedIn()`)
- **Authorization:** Publicly accessible to all signed-in tenant subscribers (`firestore.rules:77`).
- **Tenant Scope:** Mandatory filter: `.where("tenantId", "==", tenantId)`
- **Request Parameters:** `categoryIds` (optional array-contains), `featured` (optional boolean), `limit` (default 20), `startAfter` (cursor pagination).
- **Response Schema:**
```json
[
  {
    "id": "mov_c98a7sdf",
    "tenantId": "tenant_sundari",
    "title": "Sundari Chronicles",
    "description": "An epic drama following a royal dynasty.",
    "posterUrl": "https://s3.amazonaws.com/.../poster.webp",
    "backdropUrl": "https://s3.amazonaws.com/.../backdrop.webp",
    "duration": 7320,
    "releaseDate": "2026-09-01T00:00:00Z",
    "categoryIds": ["cat_drama", "cat_historical"],
    "language": "Hindi",
    "featured": true,
    "status": "PUBLISHED"
  }
]
```
- **Error Behavior:** Empty list if no items match.
- **Implementation Status:** **IMPLEMENTED** (Verified in Android `ContentRepositoryImpl.kt`).

---

### 3.2 `CAT-02: Get Content Details`
- **Mechanism:** Direct Firestore Document Read via Client SDK
- **Path:** `/content/{contentId}`
- **Calling Clients:** Android App, Consumer Web, Admin CMS
- **Authentication:** Required (`isSignedIn()`)
- **Authorization:** Permitted if `status == "PUBLISHED"` OR caller is `isTenantAdmin(tenantId)` (`firestore.rules:77-80`).
- **Tenant Scope:** Matching `tenantId`.
- **Response Schema:** Returns full `Content` document including `trailerPlaybackAsset` metadata (logical HLS path, duration).
- **Implementation Status:** **IMPLEMENTED** (Verified in `ContentDetailViewModel.kt`).

---

### 3.3 `CAT-03: List Categories`
- **Mechanism:** Direct Firestore Query via Client SDK
- **Path:** `/categories` where `tenantId == :tenantId` and `active == true`
- **Calling Clients:** Android App, Consumer Web
- **Authentication:** Required (`isSignedIn()`)
- **Authorization:** Permitted if `active == true` OR caller is tenant admin (`firestore.rules:128`).
- **Response Schema:** Array of `{ id, tenantId, name, slug, sortOrder, active }`.
- **Implementation Status:** **IMPLEMENTED** (Verified in `CategoryRepositoryImpl.kt`).

---

## 4. Media Ingestion & Administration Domain

### 4.1 `MED-01: Create Media Upload`
- **Mechanism:** Firebase Callable (`functions.https.onCall`)
- **Function Name:** `createMediaUpload`
- **Calling Clients:** Admin CMS (`/admin/content`)
- **Authentication:** Required (`context.auth.uid`)
- **Authorization:** Tenant Administrator (`verifyAdmin(uid)`)
- **Tenant Scope:** Derived server-side from `userDoc.tenantId`
- **Request Schema:**
```json
{
  "contentId": "mov_c98a7sdf",
  "mediaType": "MAIN_VIDEO", // "MAIN_VIDEO" | "TRAILER" | "POSTER" | "BACKDROP"
  "fileName": "master_feature.mov",
  "mimeType": "video/quicktime", // Allowlist: video/mp4, video/quicktime, image/jpeg, image/png, image/webp
  "fileSize": 4829104812 // In bytes, max 10GB for video, 20MB for images
}
```
- **Response Schema:**
```json
{
  "uploadId": "upl_38a7dfa0-482a-412e-9d8a-98ad7f",
  "signedUrl": "https://ott-content-bucket.s3.amazonaws.com/uploads/tenant_sundari/mov_c98a7sdf/source/main_video/upl_38a7dfa0...?AWSAccessKeyId=...",
  "sourceKey": "uploads/tenant_sundari/mov_c98a7sdf/source/main_video/upl_38a7dfa0-482a-412e-9d8a-98ad7f"
}
```
- **Error Behavior:** Throws `permission-denied` if non-admin or cross-tenant; `invalid-argument` if unsupported MIME type or excessive file size; `not-found` if content does not exist.
- **Rate Limiting:** Max 10 calls per minute per admin.
- **Implementation Status:** **IMPLEMENTED** (Verified in `functions/src/media.ts:40`).

---

### 4.2 `MED-02: Complete Media Upload`
- **Mechanism:** Firebase Callable (`functions.https.onCall`)
- **Function Name:** `completeMediaUpload`
- **Calling Clients:** Admin CMS (`/admin/content`)
- **Authentication:** Required (`context.auth.uid`)
- **Authorization:** Tenant Administrator (`verifyAdmin(uid)`)
- **Tenant Scope:** Verified against `uploadDoc.tenantId`
- **Request Schema:**
```json
{
  "uploadId": "upl_38a7dfa0-482a-412e-9d8a-98ad7f"
}
```
- **Response Schema:**
```json
{
  "status": "PROCESSING", // "PROCESSING" (video) or "READY" (image)
  "jobId": "1728562910482-mc98a7" // AWS MediaConvert Job ID if video
}
```
- **Security Check:** Performs S3 `HeadObjectCommand` to verify object exists and actual size matches declared size within 5% tolerance.
- **Idempotency:** Returns current status if already called.
- **Implementation Status:** **IMPLEMENTED** (Verified in `functions/src/media.ts:115`).

---

### 4.3 `MED-03: MediaConvert State Webhook`
- **Mechanism:** HTTPS Request Trigger (`functions.https.onRequest`)
- **Path:** `/mediaConvertWebhook`
- **Calling Clients:** AWS EventBridge via Amazon SNS Topic
- **Authentication:** Cryptographic SNS Message Signature (`sns-validator`)
- **Authorization:** Confirmed matching `MEDIACONVERT_SNS_TOPIC_ARN`
- **Tenant Scope:** Derived from internal Firestore upload record (`uploadId`)
- **Request Payload:** Standard AWS EventBridge / MediaConvert JSON notification payload containing `detail.status`, `detail.jobId`, and `detail.userMetadata.uploadId`.
- **Response:** HTTP 200 `OK` or HTTP 401 `Invalid signature` / `Invalid Topic`.
- **Implementation Status:** **IMPLEMENTED** (Verified in `functions/src/media.ts:291`).

---

### 4.4 `MED-04: Change Content Publication Status`
- **Mechanism:** Firebase Callable (`functions.https.onCall`)
- **Function Name:** `changeContentStatus`
- **Calling Clients:** Admin CMS (`/admin/content`)
- **Authentication:** Required (`context.auth.uid`)
- **Authorization:** Tenant Administrator (`verifyAdmin(uid)`)
- **Tenant Scope:** Matches `contentDoc.tenantId`
- **Request Schema:**
```json
{
  "contentId": "mov_c98a7sdf",
  "status": "PUBLISHED" // "PUBLISHED" | "DRAFT" | "ARCHIVED"
}
```
- **Response Schema:**
```json
{
  "success": true
}
```
- **Publication Readiness Gate:** If `status == "PUBLISHED"`, verifies that `mainPlaybackAsset.hlsPath` is non-empty and title/description are present. Throws `failed-precondition` if video is unready.
- **Implementation Status:** **IMPLEMENTED** (Verified in `functions/src/media.ts:424`).

---

## 5. Playback Authorization Domain

### 5.1 `PLY-01: Authorize Video Playback`
- **Mechanism:** Firebase Callable (`functions.https.onCall`)
- **Function Name:** `authorizePlayback`
- **Calling Clients:** Android App (ExoPlayer), Consumer Web (HLS Player)
- **Authentication:** Required (`context.auth.uid` via verified Firebase ID Token)
- **Authorization:** Server-authoritative entitlement check (trailers public to authenticated users; main video strictly requires active subscription).
- **Tenant Scope:** Validates `userDoc.tenantId === contentDoc.tenantId`.
- **Rate Limiting:** Enforces `RATE_LIMIT_MAX_CALLS = 20` per 60 seconds per user in `_rateLimits` collection.
- **Request Schema:**
```json
{
  "contentId": "mov_c98a7sdf",
  "isTrailer": false
}
```
- **Response Schema:**
```json
{
  "authorized": true,
  "playbackUrl": "https://d111111abcdef8.cloudfront.net/content/tenant_sundari/mov_c98a7sdf/main/hls/upl_38a7dfa0.m3u8",
  "expiresAt": 1728563810000,
  "cookies": {
    "CloudFront-Policy": "eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9kMTExMTExYWJjZGVmOC5jbG91ZGZyb250Lm5ldC9jb250ZW50L3RlbmFudF9zdW5kYXJpL21vdl9jOThhN3NkZi9tYWluL2hscy8qIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzI4NTYzODEwfX19XX0_",
    "CloudFront-Signature": "iK8z...==",
    "CloudFront-Key-Pair-Id": "K2JC39QW345DEF"
  }
}
```
- **Error Behavior:**
  - `unauthenticated`: Missing auth token.
  - `invalid-argument`: Malformed `contentId` or path traversal detected (`..`, `/`, `\`).
  - `resource-exhausted`: Throttled (> 20 requests/minute).
  - `not-found`: Content does not exist.
  - `failed-precondition`: Content not published or CloudFront unconfigured.
  - `permission-denied`: Tenant mismatch or missing active subscription (`SUBSCRIPTION_REQUIRED`).
- **Implementation Status:** **IMPLEMENTED** (Verified in `functions/src/playback.ts:42`, 20 passing unit tests).

---

## 6. Subscriptions & Payments Domain

### 6.1 `SUB-01: List Subscription Plans`
- **Mechanism:** Direct Firestore Query via Client SDK
- **Path:** `/plans` where `tenantId == :tenantId` and `active == true`
- **Calling Clients:** Android App, Consumer Web
- **Authentication:** Required (`isSignedIn()`)
- **Authorization:** Authenticated subscribers (`firestore.rules:141`)
- **Response Schema:**
```json
[
  {
    "id": "plan_monthly_inr",
    "tenantId": "tenant_sundari",
    "name": "Standard Monthly",
    "description": "Unlimited HD streaming for 30 days",
    "price": 199.0,
    "currency": "INR",
    "billingPeriod": "monthly",
    "active": true
  }
]
```
- **Implementation Status:** **IMPLEMENTED** (Verified in `PlanRepositoryImpl.kt`).

---

### 6.2 `SUB-02: Payment Gateway Webhook Receiver`
- **Mechanism:** HTTPS Request Trigger (`functions.https.onRequest`)
- **Path:** `/paymentWebhook`
- **Calling Clients:** External Payment Gateway (Stripe / Razorpay)
- **Authentication:** Cryptographic HMAC-SHA256 signature over raw request body (`req.rawBody`)
- **Authorization:** Verified against `GATEWAY_WEBHOOK_SECRET` stored in Firebase Secret Manager.
- **Security Check:** Timing-safe buffer comparison (`crypto.timingSafeEqual`).
- **Idempotency:** Firestore transaction on `/webhook_events/{eventId}` ensures events are processed exactly once.
- **Tenant Scope:** Verified against target user profile (`userDoc.tenantId === event.metadata.tenantId`).
- **Request Headers:** Provider-specific signature header (e.g., `Stripe-Signature` or `x-gateway-signature`).
- **Payload Schema:** Gateway event object containing `id`, `type`, `data.subscription_id`, and `data.metadata: { userId, tenantId, planId }`.
- **Response:** HTTP 200 `Webhook received` (or HTTP 401 for bad signature).
- **Implementation Status:** **IMPLEMENTED** (Generic prototype verified in `functions/src/index.ts:68`; provider driver pending Phase 5).

---

## 7. API Versioning Strategy

1. **Callable Functions:** Callable function interfaces maintain backward compatibility by accepting optional parameters. Breaking contract modifications will be introduced under versioned namespaces (e.g. `authorizePlaybackV2`).
2. **Webhook Endpoints:** Webhook paths support provider-specific routing:
   - `/paymentWebhook` (Legacy / Default)
   - `/webhooks/stripe` (Dedicated Stripe adapter)
   - `/webhooks/razorpay` (Dedicated Razorpay adapter)
   - `/mediaConvertWebhook` (AWS EventBridge notifications)
3. **Firestore Schema Versioning:** Entities include a `schemaVersion: 1` field in root documents to enable future lazy migrations if fields are deprecated.
