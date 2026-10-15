# ADR-007: Multi-Tenant Logical Isolation and Tenant Scoping

- **Status:** PROPOSED (Architectural Decision & Specification)
- **Date:** 2026-10-16
- **Authors:** Principal Software Architect, Application Security Architect

---

## 1. Context and Problem Statement

The platform is designed as an OTT SaaS platform capable of serving multiple independent streaming brands or tenants.
We must establish how tenant isolation is modeled, enforced, and audited across:
- Cloud Firestore documents and queries.
- AWS S3 object keys and storage paths.
- CloudFront signed cookie scopes.
- Client applications (Android mobile app and Web application).
- Administrative operations and RBAC.

We must decide between physical multi-tenancy (separate databases and AWS accounts per tenant) versus logical multi-tenancy (shared infrastructure partitioned by `tenantId`).

---

## 2. Options Considered

### Option 1: Logical Partitioning by `tenantId` (Shared Database and S3 Bucket)
- All Firestore collections (`users`, `content`, `subscriptions`, `plans`, `categories`, `mediaUploads`) contain a mandatory `tenantId` field.
- Firestore security rules strictly filter and enforce tenant matching on reads and writes.
- S3 objects follow a deterministic hierarchical prefix: `content/{tenantId}/{contentId}/...` and `uploads/{tenantId}/{contentId}/...`.
- CloudFront signed cookies are locked to the specific tenant and content path prefix: `content/{tenantId}/{contentId}/{main|trailer}/hls/*`.
- Single set of Cloud Functions and AWS infrastructure, with zero per-tenant baseline cost.

### Option 2: Physical Isolation (Separate Database / S3 Bucket / Firebase Project per Tenant)
- Each tenant is provisioned an isolated Firebase project and AWS account.
- **Pros:** Total physical isolation.
- **Cons:** Astronomical operational complexity for early-stage operations; multiplied infrastructure costs; difficult to deploy platform updates across hundreds of independent projects.

---

## 3. Decision and Recommendation

**Adopt Option 1: Logical Multi-Tenancy Partitioning by `tenantId`.**

### Enforcement Mechanisms:
1. **Firestore Client-Side Rules:**
   - Helper function `getUserTenant()` reads the authenticated user's `tenantId` from `/users/{uid}`.
   - For administrative actions, `isTenantAdmin(tenantId)` requires `isAdmin() && getUserTenant() == tenantId`.
   - Content mutations require `isTenantAdmin(resource.data.tenantId)` and prohibit altering `tenantId` via immutable diff checks.
   - Clients cannot self-assign or mutate `tenantId` on their own user profiles (`!("tenantId" in request.resource.data)`).
2. **Serverless Functions Authorization:**
   - In `authorizePlayback`: The function loads the content document, extracts `contentDoc.data().tenantId`, loads `userDoc.data().tenantId`, and throws `permission-denied (CROSS_TENANT_ACCESS_DENIED)` if they do not match.
   - In `createMediaUpload`: Derives `tenantId` strictly from `userDoc.data().tenantId` and verifies `contentDoc.data().tenantId === tenantId`. S3 key is formatted server-side as `uploads/{tenantId}/{contentId}/...`.
   - In `mediaConvertWebhook`: Output HLS destination is derived as `content/{tenantId}/{contentId}/...` using database records, never trusting untrusted payload segments.
3. **Delivery Cookie Scoping:**
   - The signed cookie resource URL is restricted to `https://${CLOUDFRONT_DOMAIN}/content/${tenantId}/${contentId}/${assetDir}/hls/*`. Cross-tenant or cross-content URL access is rejected by CloudFront edge servers.
4. **Tenant Assignment on Registration (Consumer Web/App):**
   - The Android app is branded for a specific tenant via `BuildConfig.TENANT_ID`.
   - The Web application derives `tenantId` from the hostname/subdomain (e.g., `tenant1.streaming.com` or custom domain mapping in `/tenants/{tenantId}`).
   - An authenticated cloud function (`assignUserTenant` or registration trigger) writes `tenantId` to the user profile upon first registration.

---

## 4. Consequences and Mitigations

### Positive:
- Minimal infrastructure footprint and cost.
- Shared code and unified deployments across all tenants.
- Strong cryptographic and rule-based boundaries prevent data leakage.

### Negative / Risks:
- "Noisy Neighbor" risk: Mitigated by per-user rate limiting in Cloud Functions and CloudFront edge caching.
- Query performance: Firestore queries must always include `.where("tenantId", "==", tenantId)` and require composite indexes.

---

## 5. Evidence and Approval Status

- **Evidence:** `firestore.rules:26-32`, `functions/src/playback.ts:94-104`, `functions/src/media.ts:57-60`.
- **Approval Status:** **ESTABLISHED DIRECTION — SPECIFICATION COMPLETE**.
