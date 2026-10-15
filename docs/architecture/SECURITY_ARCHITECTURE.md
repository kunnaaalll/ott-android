# Unified OTT Platform — Security & Trust Boundary Architecture

- **Document Version:** 1.0.0
- **Document Status:** PROPOSED THREAT-INFORMED SPECIFICATION (Phase 2 Deliverable)
- **Author:** Principal Application Security Architect, Cloud Security Lead
- **Security Posture:** Zero-Trust Client Execution, Server-Authoritative Entitlements, Multi-Tenant Cryptographic Isolation.

---

## 1. System Trust Boundaries & Threat Model

The platform processes premium copyrighted video media, recurring subscription billing, and user personal data. The threat model assumes that:
1. **Client Run-Times are Untrusted:** The native Android application and web browser run on untrusted consumer hardware. Clients can decompile bytecode, inspect network traffic, alter in-memory states, or inject arbitrary parameters.
2. **Database Direct Access is Constrained:** Clients interact directly with Cloud Firestore; hence, database rules (`firestore.rules`) form a first line of defense that must reject any untrusted write, privilege escalation, or cross-tenant query.
3. **Media Storage is Strictly Air-Gapped from Public Traffic:** Amazon S3 blocks all public access. The only egress path is via Amazon CloudFront using Origin Access Control (OAC), guarded by RSA-2048 cryptographically signed cookies.
4. **Third-Party Integrations Require Cryptographic Authenticity:** Inbound webhooks from payment gateways and AWS MediaConvert arrive over public internet HTTPS; every inbound event must be cryptographically authenticated using HMAC signatures or SNS certificates before processing.

### 1.1 Architectural Trust Boundaries Diagram

```mermaid
graph TB
    subgraph UntrustedZone ["Untrusted Zone (Public Internet & Client Hardware)"]
        Attacker["Potential Adversary / Malicious Client"]
        ClientApp["Android App / Web Browser"]
    end

    subgraph EdgeBoundary ["Edge Security Boundary (AWS CloudFront)"]
        CFEdge["CloudFront Edge Servers\n(OAC + Signed Cookie Guard)"]
    end

    subgraph ServerlessBoundary ["Backend Trust Boundary (Google Cloud & Firebase)"]
        AuthSvc["Firebase Authentication\n(JWT Token Issuer)"]
        FunctionsSvc["Firebase Cloud Functions\n(Server-Authoritative Enforcement)"]
        RulesEngine["Firestore Security Rules Engine\n(Default Deny, Field-Level Write Locks)"]
        SecretMgr["Firebase Secret Manager\n(Private Keys & HMAC Secrets)"]
    end

    subgraph ProtectedStorageBoundary ["Protected Cloud Storage (Private AWS)"]
        S3Bucket["Private Amazon S3\n(Mezzanine & HLS Segments)"]
        MediaConvertSvc["AWS MediaConvert Transcoder"]
    end

    %% Flow and Trust Crossings
    ClientApp -->|1. Firebase Auth (TLS)| AuthSvc
    ClientApp -->|2. Direct Firestore (Guarded by Rules)| RulesEngine
    ClientApp -->|3. Callables with ID Token| FunctionsSvc
    FunctionsSvc -->|Load Secrets| SecretMgr
    FunctionsSvc -->|Sign CloudFront Cookies| ClientApp
    ClientApp -->|4. HLS Stream Request with Cookies| CFEdge
    CFEdge -->|5. Verify Cookie Signature| CFEdge
    CFEdge -->|6. OAC Authenticated GetObject| S3Bucket
    FunctionsSvc -->|7. Pre-sign S3 PUT & Trigger Transcode| S3Bucket
    S3Bucket -->|Source Video| MediaConvertSvc
    MediaConvertSvc -->|Transcoded Segments| S3Bucket

    %% Threat Points
    Attacker -.->|Threat A: Forged Admin Claim| RulesEngine
    Attacker -.->|Threat B: Path Traversal Attack| FunctionsSvc
    Attacker -.->|Threat C: Forged Payment Webhook| FunctionsSvc
    Attacker -.->|Threat D: Direct S3 Media Access| S3Bucket
```

---

## 2. Core Security Domains & Controls

### 2.1 Identity & Authorization Boundary
- **Token Verification:** Every Cloud Function callable validates the cryptographically signed Firebase ID token (`context.auth.uid`). Requests lacking a valid token throw `unauthenticated` (HTTP 401).
- **Server-Authoritative Admin Verification:** The platform explicitly rejects client-claimed admin headers or parameters. Function `verifyAdmin(uid)` in `functions/src/media.ts:31` performs a direct Firestore read on `/users/{uid}` and requires `data.isAdmin === true` and a valid `tenantId`.
- **Client Privilege Escalation Lock:** `firestore.rules:43-44` enforces that standard users can create profile documents, but any payload containing `"isAdmin"` or `"tenantId"` is rejected.
- **Tenant Isolation Enforcement:** Every data record is tagged with `tenantId`. Cloud Functions verify that the caller's database `tenantId` strictly matches the target content's `tenantId` before proceeding.

---

### 2.2 Payment Webhook Verification & Idempotency
- **Raw-Body Preservation:** Webhook triggers enforce that the raw, unmodified request buffer (`req.rawBody`) is preserved. Cryptographic signature calculations are performed against this exact buffer.
- **Timing-Safe HMAC Verification:** In `functions/src/index.ts:58`, incoming signatures are compared to expected HMAC-SHA256 digests using Node's `crypto.timingSafeEqual()`:
  ```typescript
  return crypto.timingSafeEqual(
      Buffer.from(signature, "hex"),
      Buffer.from(expectedSignature, "hex")
  );
  ```
  This prevents side-channel timing attacks that could deduce secret bytes from execution time variations.
- **Fail-Closed Secret Configuration:** If `GATEWAY_WEBHOOK_SECRET` is unset, `verifySignature()` logs an error and returns `false`, causing all requests to fail closed with HTTP 401 (`index.ts:31-35`).
- **Atomic Transaction Idempotency:** Webhook event IDs are tracked in `/webhook_events/{eventId}` within a Firestore transaction (`index.ts:88-96`). If an event was already processed, it is safely acknowledged with HTTP 200 without executing duplicate subscription extensions.
- **Cross-Tenant Subscription Poisoning Guard:** The webhook handler verifies that `userDoc.data().tenantId === event.data.metadata.tenantId`. If a webhook attempts to assign a subscription to a user belonging to another tenant, it is halted and logged (`index.ts:124-128`).
- **No Client Writes to Subscriptions:** `firestore.rules:67` enforces `allow write: if false` on `/subscriptions/{id}`. Only backend Admin SDK can write subscription activations.

---

### 2.3 Media Storage & Ingestion Security
- **Private S3 Storage:** The content S3 bucket (`staging/main.tf:29-36`) has `block_public_acls`, `block_public_policy`, `ignore_public_acls`, and `restrict_public_buckets` all set to `true`. Bucket ownership is locked to `BucketOwnerEnforced`.
- **S3 Bucket Policy Explicit Deny:** The bucket policy contains a statement explicitly denying `s3:GetObject` to any principal whose `AWS:SourceArn` does not match the authorized CloudFront distribution ARN (`staging/main.tf:107-117`). Direct access to S3 is impossible even if public access block is misconfigured.
- **Single-Use Scoped Upload URLs:** S3 upload pre-signatures are generated with an expiration of 1 hour (`media.ts:90`) and bound to a deterministic, tenant-isolated path: `uploads/{tenantId}/{contentId}/source/{mediaType}/{uploadId}`.
- **MIME & Size Whitelist:** Upload pre-signing rejects any MIME type outside `video/mp4` and `video/quicktime` (max 10GB) or `image/jpeg`, `image/png`, and `image/webp` (max 20MB).
- **HeadObject Upload Verification:** Prior to triggering AWS MediaConvert, `completeMediaUpload` executes S3 `HeadObject` (`media.ts:145`) to verify that the file actually exists, is non-empty, and matches the declared size within a 5% tolerance.
- **Cryptographic SNS Verification:** Inbound MediaConvert notifications are validated using `sns-validator` (`media.ts:300`), which fetches the signing certificate from trusted Amazon domains, verifies the signature, and matches the topic against `MEDIACONVERT_SNS_TOPIC_ARN`.
- **Stale Transcode Drop:** In `media.ts:367`, callbacks with a mismatched `jobId` are dropped to prevent stale or out-of-order transcode events from corrupting database records.

---

### 2.4 Video Playback & CloudFront Signed Cookies
- **Server-Authoritative Entitlement Verification:** In `playback.ts:107`, the backend checks Firestore `/subscriptions` for active status and verifies that `expiryDate.toMillis() > now.toMillis()`. Client device clocks are ignored.
- **Path Traversal Protection:** Request parameters and internal HLS paths are inspected for traversal attempts (`playback.ts:58, 159`). Characters like `..`, `/`, `\`, and `%2e%2e` are rejected with `invalid-argument`. Paths must match strict regex: `/^[a-zA-Z0-9/_.\-]+$/`.
- **Prefix Lockout:** The internal HLS path must strictly start with the expected prefix: `content/{tenantId}/{contentId}/{assetDir}/hls/`. Any deviation indicates database corruption or insider tampering and is rejected (`playback.ts:147`).
- **Narrow Cookie Resource Scope:** CloudFront signed cookies (`CloudFront-Policy`, `CloudFront-Signature`, `CloudFront-Key-Pair-Id`) are generated using an RSA-2048 private key. The resource URL is scoped narrowly to `https://${CLOUDFRONT_DOMAIN}/content/${tenantId}/${contentId}/${assetDir}/hls/*` with a 15-minute expiration (`playback.ts:190-194`).
  - A trailer cookie cannot access main video assets.
  - A cookie for Content A cannot access Content B.
  - A cookie for Tenant A cannot access Tenant B assets.
- **Zero Cloud Credentials on Clients:** No AWS IAM keys, S3 credentials, or CloudFront private keys are ever distributed to the Android APK or web client bundle. The client receives only the short-lived signed cookie string.

---

### 2.5 Secrets & Environment Hygiene
- **Firebase Secret Manager Integration:** All production credentials (`GATEWAY_WEBHOOK_SECRET`, `CLOUDFRONT_PRIVATE_KEY`, `CLOUDFRONT_KEY_PAIR_ID`, `MEDIACONVERT_SNS_TOPIC_ARN`) are provisioned via `firebase functions:secrets:set` and mounted into function runtimes.
- **Source Control Scanning:** No live credentials or private keys exist in version control. All `.env*`, `*.pem`, `*.key`, `*.keystore`, and service-account JSON files are properly ignored in `.gitignore`.
- **Fail-Safe Preconditions:** OpenTofu staging configurations enforce that `force_destroy = true` is strictly restricted to environments where `environment == "staging"` and the bucket name contains `"staging"` (`staging/main.tf:22-26`).

---

## 3. Threat-Informed Security Requirements Matrix

| Threat ID | Threat / Attack Scenario | Affected Asset | Attack Vector | Required Security Control | Enforcement Point | Verification Test Scenario | Current Evidence | Remaining Gap | Remediation Phase |
|---|---|---|---|---|---|---|---|---|---|
| **THR-01** | Client Self-Promotion to Admin | User Profile | Client crafts Firestore write setting `isAdmin: true` | Declarative write lock rejecting `isAdmin` in client resource data | `firestore.rules:43` | Attempt client SDK write with `{ isAdmin: true }` | Verified in `firestore.rules` syntax review | Dedicated Firestore rules unit test suite | Phase 3 |
| **THR-02** | Cross-Tenant Data Infiltration | User Profiles & Content | Client crafts query for another tenant's draft content | Rules mandate `getUserTenant() == tenantId` for drafts | `firestore.rules:79` | Query content with Tenant B ID using Tenant A auth token | Verified in `firestore.rules` | Rules test harness in Emulator | Phase 3 |
| **THR-03** | Webhook Forgery & Tampering | Subscriptions | Attacker posts fake `payment.success` event | HMAC-SHA256 signature verification over raw body | `functions/src/index.ts:58` | Issue HTTP POST with modified payload or forged signature | Verified in `functions/src/index.ts` (Passes unit test) | Provider-specific header integration | Phase 5 |
| **THR-04** | Webhook Replay Attack | Subscriptions | Attacker re-sends intercepted valid webhook payload | Firestore transaction on `/webhook_events/{eventId}` | `functions/src/index.ts:88` | Send identical valid webhook twice | Verified in `functions/src/index.ts` (Passes unit test) | End-to-end gateway test in staging | Phase 5 / Phase 9 |
| **THR-05** | Unauthorized Video Stream Ripping | Premium HLS Video | Attacker directly downloads `.m3u8` or `.ts` chunks | CloudFront signed cookies with RSA-2048 signature + S3 OAC | CloudFront Edge & `playback.ts:190` | Request HLS URL via curl without valid CloudFront cookies | Verified in `staging/main.tf` & `playback.test.ts` (20 tests) | Staging live CloudFront test | Phase 4 / Phase 9 |
| **THR-06** | Trailer-to-Main Privilege Escalation | Premium HLS Video | Attacker uses trailer cookie to fetch main feature | Signed cookie resource URL locked to `/trailer/hls/*` vs `/main/hls/*` | `playback.ts:190` | Request `/main/hls/master.m3u8` using cookies from `/trailer/` | Verified in `playback.ts` scoping logic | Playback test suite | Phase 4 / Phase 9 |
| **THR-07** | Relative Path Traversal Injection | Local & S3 Storage | Injected `../../` in `contentId` or `hlsPath` | Regex and substring traversal validation | `playback.ts:58, 159` | Call `authorizePlayback` with `contentId: "../../etc/passwd"` | Verified in `playback.test.ts` (Rejection tested) | Complete | Phase 1 (Verified) |
| **THR-08** | Ingestion of Malicious Executables | S3 Bucket | Admin uploads malware binary disguised as video | MIME allowlist and file size constraints | `media.ts:63-75` | Call `createMediaUpload` with `mimeType: "application/x-sh"` | Verified in `media.test.ts` (Rejection tested) | Complete | Phase 1 (Verified) |
| **THR-09** | CloudFront Private Key Exposure | Video Delivery | Key checked into Git or returned to client | Secrets loaded from Secret Manager; only cookies returned | `playback.ts:234` | Scan repo for `.pem`; verify API response schema | Verified in `playback.ts` and Git history | Continuous secret scanning in CI | Phase 3 |
| **THR-10** | Staged Bucket Accidental Purge | S3 Video Bucket | Accidental `force_destroy` executed against prod | OpenTofu lifecycle precondition validating staging name | `staging/main.tf:22` | Run `tofu apply` with prod bucket name and `force_destroy=true` | Verified in `staging/main.tf` syntax | Precondition verified in OpenTofu | Phase 1 (Verified) |
