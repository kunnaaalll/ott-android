# Phase 2 — Architecture & Requirements Consolidation Report

- **Execution Date:** 2026-10-16
- **Lead System Architect:** Principal Software Architect, Application Security Architect, Cloud Infrastructure Architect
- **Scope:** Multi-Repository Workspace (`Android - OTT`, `Backend`, `Website`), Architectural Synthesis, Logical API Contracts, Data Model & Ownership, Security & Threat Model, Nonfunctional Requirements, Cost Drivers, Phased Roadmap.
- **Phase Verdict:** **PARTIAL — ARCHITECTURE PROPOSED, DECISIONS OUTSTANDING**

---

## 1. Executive Summary

Phase 2 establishes the definitive technical blueprint and system architecture for the unified OTT streaming platform. Grounded in direct inspection of the codebase, reproducible validation from Phase 1, and the product principles established in `MASTER.md`, this phase consolidates requirements across all domains without performing premature application scaffolding or live cloud modifications.

### Key Architectural Outcomes
1. **Authoritative Component & Repository Mapping:** Formally mapped the three Git repositories. The active codebase currently lives in `Android - OTT/` (`ott-android.git`). The standalone `ott-backend.git` repository (`Backend/`) and `ott-website.git` (`Website/`) are confirmed clean and empty. Recommended migrating the serverless Cloud Functions backend from `Android - OTT/functions` into `ott-backend` during Phase 3 to establish clean domain ownership, autonomous CI/CD, and multi-client parity (ADR-003).
2. **Unified Web Platform Architecture:** Established the architecture for a single, shared web application codebase (`Website/`) hosting both consumer streaming routes and protected administrative routes under `/admin` (ADR-001). This eliminates duplicate design systems, auth clients, and hosting overhead.
3. **Validated Serverless Platform:** Reconfirmed serverless Firebase Cloud Functions and Cloud Firestore as the application platform (ADR-002, ADR-004), rejecting expensive container/VPS clusters in accordance with Principle 2.1 ("Simple Infrastructure").
4. **End-to-End Media & Playback Pipeline:** Fully mapped the AWS media architecture (ADR-005). Private S3 mezzanine storage, automated MediaConvert HLS transcoding, CloudFront Origin Access Control (OAC), and 15-minute RSA-2048 Signed Cookies (`playback.ts:181`) enforce cryptographic access control without distributing cloud credentials to clients.
5. **Security & Threat Model:** Formalized the STRIDE-based security architecture, verifying timing-safe webhook HMAC verification (`index.ts:58`), fail-closed secrets hygiene, S3 lifecycle rules, and path traversal guards (`playback.ts:58`).
6. **Cost Architecture & Economics:** Identified the 4 primary cost drivers, highlighting that CloudFront data transfer out is the dominant operational expense. Proposed capping mobile video streams at 720p to cut per-subscriber delivery costs from ~$2.87 to ~$1.40/month.
7. **Phase-Gated Implementation Roadmap:** Formulated a dependency-aware 8-phase execution plan. Confirmed the absolute invariant that **Phase 9 is strictly reserved for live cloud deployment and live-application acceptance testing.**

---

## 2. Repositories & Workspace Inspected

Direct workspace inspection verified the following active Git roots:

| Repository Directory | Git Remote Origin | Active Branch | Commit Hash | Working Tree Status | Role / Responsibility |
|---|---|---|---|---|---|
| `/Applications/My Mac/OTT/Android - OTT` | `https://github.com/kunnaaalll/ott-android.git` | `main` | `70aa8f9` | `M firestore.rules`, `?? docs/`, `?? firebase.json` | Active primary repository hosting the Android application, Cloud Functions backend (`functions/`), OpenTofu staging infrastructure (`staging/`), and Firestore rules. |
| `/Applications/My Mac/OTT/Backend` | `https://github.com/kunnaaalll/ott-backend.git` | `main` | No commits yet | Clean (0 files) | Target home for the decoupled serverless backend, Firestore rules, and backend CI/CD pipeline. |
| `/Applications/My Mac/OTT/Website` | `https://github.com/kunnaaalll/ott-website.git` | `main` | No commits yet | Clean (0 files) | Target home for the unified web application (consumer streaming + `/admin` CMS). |
| `/Applications/My Mac/OTT/docs` | Symlink to `Android - OTT/docs` | N/A | N/A | Active | Centralized engineering, security, and architecture documentation root. |

---

## 3. Existing Capabilities vs. Missing Capabilities

A comprehensive audit of the codebase yielded the following capability baseline:

```
[==================== IMPLEMENTED & VERIFIED (65%) ====================]
- Android App UI & Navigation (47 Passing Unit Tests)
- Android ExoPlayer Media3 Signed-Cookie Video Player
- Firebase Cloud Functions Backend (35 Passing Unit Tests)
- Secure Media Upload Pre-Signing (S3 PUT 1-hour presign)
- Upload Verification via S3 HeadObject (5% size tolerance)
- AWS MediaConvert HLS Transcoding Pipeline Configuration
- SNS Transcode Notification Processing (Cryptographic verification)
- Server-Authoritative Content Publication Readiness Gate
- CloudFront RSA-2048 Signed-Cookie Playback Authorization
- Timing-Safe Webhook HMAC Verification (crypto.timingSafeEqual)
- Webhook Firestore Transaction Idempotency
- Cross-Tenant Isolation in Playback and Webhooks
- OpenTofu Staging Infrastructure as Code (tofu validate passes)
- Firestore Security Rules Default-Deny & Client Field Locks

[======================= MISSING / GAPS (35%) =========================]
- Consumer Streaming Web Application (Website/ is empty)
- Web Administrative CMS under /admin (Website/ is empty)
- Series, Seasons & Episode Hierarchy in Data Model & Backend API
- Provider-Specific Payment Gateway Driver (Stripe/Razorpay unfinalized)
- Dedicated Server-Authoritative Tenant Assignment Cloud Function
- Cross-Device Watch Progress / Resume Playback Collection
- Full-Text Catalog Search (Algolia integration / Firestore composite index)
- Dedicated Firestore Rules Automated Test Harness
```

---

## 4. Summary of Key Architectural Decisions (ADRs)

Detailed Decision Records are stored in `docs/architecture/adr/`:

- **ADR-001 (Unified Web Codebase with Integrated `/admin`):** Single Next.js/React codebase in `Website/` serving both public streaming routes and protected `/admin` CMS routes. Eliminates duplicate design systems, auth clients, and hosting pipelines.
- **ADR-002 (Serverless Backend Runtime):** Retain Firebase Cloud Functions (Node.js 18). Avoids expensive $35–$120/mo container/VPS cluster baselines, maintains $0.00 idle costs, and leverages existing 35 passing unit tests.
- **ADR-003 (Backend Repository Ownership):** Recommend migrating backend source from `Android - OTT/functions` into `Backend/` (`ott-backend.git`) during Phase 3 to establish clean separation of concerns and independent CI/CD.
- **ADR-004 (Firebase Auth & Cloud Firestore Platform):** Reconfirmed managed Firebase platform. Leverages declarative field-level security rules and real-time client listeners for subscription sync.
- **ADR-005 (AWS S3, MediaConvert, and CloudFront Media Stack):** Private S3 storage, automated HLS transcoding, and 15-minute CloudFront signed cookies. Zero cloud credentials in client applications.
- **ADR-006 (Payment Gateway Abstraction):** Standardized canonical `SubscriptionEvent` and strict raw-body HMAC verification with `crypto.timingSafeEqual()`. Decouples backend from specific payment vendor.
- **ADR-007 (Logical Multi-Tenant Partitioning):** Shared cloud infrastructure partitioned logically by `tenantId` across Firestore documents, S3 keys (`content/{tenantId}/...`), and CloudFront signed cookie scopes.
- **ADR-008 (Multi-Environment Isolation & Phase 9 Live Gate):** Strict project boundaries between local, staging, and production. Phase 9 formally reserved for all live deployment and live-system validation.

---

## 5. Security Architecture & Threat Highlights

The security architecture enforces a strict zero-trust posture:
1. **Timing-Safe HMAC:** Webhooks reject altered payloads and compare signatures via constant-time buffer comparison (`crypto.timingSafeEqual`) to eliminate timing side-channel attacks.
2. **Fail-Closed Secrets:** Endpoints verify that required signing keys exist before processing. If `GATEWAY_WEBHOOK_SECRET` or `CLOUDFRONT_PRIVATE_KEY` is missing, endpoints fail closed with HTTP 401/412.
3. **Strict Path Traversal Guards:** Function `authorizePlayback` rejects `..`, `/`, `\`, and `%2e%2e` in `contentId` and verifies that internal HLS paths match expected tenant prefixes before signing cookies.
4. **S3 Public Air-Gap:** Content S3 buckets have all public access blocks enabled, enforce `BucketOwnerEnforced`, and contain bucket policy explicit-deny rules for any requests not originating from the authorized CloudFront OAC distribution.
5. **Staging Destruction Guard:** OpenTofu configurations enforce a lifecycle precondition ensuring `force_destroy = true` can only be evaluated when `environment == "staging"` and the bucket name explicitly contains `"staging"`.

---

## 6. Phase 1 Blockers Carried Forward

The following external prerequisites remain open from Phase 1 and are explicitly tracked in the implementation roadmap:

| Blocker ID | Affected Subsystem | Description & Prerequisite | Impacted Phase | Required Action |
|---|---|---|---|---|
| **BLK-01** | Android Native App | `google-services.json` missing in `app/` | Phase 7 | Operator must supply Firebase staging `google-services.json` and uncomment plugins. |
| **BLK-02** | Android Native App | Release signing keystore unconfigured | Phase 7 | Release engineering must configure signing keystore in `app/build.gradle.kts`. |
| **BLK-03** | Android Testing | No running Android emulator / device | Phase 7 | Operator must launch an Android API 34 emulator for `./gradlew connectedAndroidTest`. |
| **BLK-04** | Backend Testing | Firebase Emulator Suite not running | Phase 3 | Operator must install Firebase CLI to run local integration test suite. |
| **BLK-05** | AWS Staging | AWS IAM credentials missing for `tofu plan` | Phase 4 | Operator must provide valid AWS credentials (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`). |
| **BLK-06** | Repositories | `ott-backend` and `ott-website` unpopulated | Phase 3 & 6 | Phase 3 migrates backend into `Backend/`; Phase 6 scaffolds web platform into `Website/`. |

---

## 7. Decisions Requiring Operator Approval

Before Phase 3 execution begins, the operator must approve:

1. **Approval of ADR-003 (Backend Repository Migration):**
   - *Question:* Do you approve migrating `functions/`, `firestore.rules`, and `firebase.json` from `Android - OTT/` into the standalone `ott-backend` repository (`Backend/`) during Phase 3?
   - *Recommendation:* **YES (Recommended)**.
2. **Payment Provider Finalization (ADR-006):**
   - *Question:* Which primary payment gateway should be integrated for subscription billing?
   - *Options:* Stripe, Razorpay, or Google Play Billing / Apple In-App Purchase.
3. **Initial Catalog & Cost Assumptions:**
   - *Question:* Do you approve the initial cost model assumptions (100 hours of initial catalog, 1,000 subscriber initial capacity, mobile default capped at 720p HD)?
   - *Recommendation:* **YES (Recommended)**.

---

## 8. Artifacts Created During Phase 2

All deliverables have been written to `docs/architecture/`:

| File Path | Description |
|---|---|
| [SYSTEM_ARCHITECTURE.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/SYSTEM_ARCHITECTURE.md) | Document 1: Unified Logical Architecture Specification with 6 Mermaid diagrams |
| [CAPABILITY_AND_REQUIREMENTS_MATRIX.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/CAPABILITY_AND_REQUIREMENTS_MATRIX.md) | Document 2: Complete Capability Matrix across 8 operational domains |
| [API_CONTRACT.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/API_CONTRACT.md) | Document 3: Unified Logical API Contract (Callables, Webhooks, Firestore Rules) |
| [DATA_MODEL_AND_OWNERSHIP.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/DATA_MODEL_AND_OWNERSHIP.md) | Document 4: Data Model, Collection Hierarchy & Client/Server Write Boundaries |
| [SECURITY_ARCHITECTURE.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/SECURITY_ARCHITECTURE.md) | Document 5: Threat Model, STRIDE Matrix & Cryptographic Trust Boundaries |
| [ENVIRONMENT_AND_DEPLOYMENT_STRATEGY.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/ENVIRONMENT_AND_DEPLOYMENT_STRATEGY.md) | Document 6: 4-Tier Topology, 12-Stage CI/CD Pipeline & Phase 9 Reservation |
| [COST_AND_NONFUNCTIONAL_REQUIREMENTS.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/COST_AND_NONFUNCTIONAL_REQUIREMENTS.md) | Document 7: NFR Framework, 4 Cost Drivers & Bandwidth Optimization Model |
| [IMPLEMENTATION_ROADMAP.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/IMPLEMENTATION_ROADMAP.md) | Document 8: Phased Implementation Roadmap (Phases 2 through 9) with Exit Gates |
| [PHASE_2_ARCHITECTURE_REPORT.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/PHASE_2_ARCHITECTURE_REPORT.md) | Document 9: Definitive Phase 2 Architecture and Consolidation Report |
| [requirements_inventory.json](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/requirements_inventory.json) | Machine-readable requirements inventory artifact |
| [ADR-001_UNIFIED_WEB_CODEBASE.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/adr/ADR-001_UNIFIED_WEB_CODEBASE.md) | Architecture Decision Record: Unified Web Codebase with Integrated `/admin` |
| [ADR-002_SERVERLESS_BACKEND_RUNTIME.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/adr/ADR-002_SERVERLESS_BACKEND_RUNTIME.md) | Architecture Decision Record: Serverless Backend Runtime (Cloud Functions) |
| [ADR-003_BACKEND_REPOSITORY_OWNERSHIP.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/adr/ADR-003_BACKEND_REPOSITORY_OWNERSHIP.md) | Architecture Decision Record: Backend Source Repository Ownership |
| [ADR-004_FIREBASE_AUTH_AND_FIRESTORE.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/adr/ADR-004_FIREBASE_AUTH_AND_FIRESTORE.md) | Architecture Decision Record: Firebase Auth and Cloud Firestore Platform |
| [ADR-005_AWS_MEDIA_STORAGE_AND_DELIVERY.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/adr/ADR-005_AWS_MEDIA_STORAGE_AND_DELIVERY.md) | Architecture Decision Record: AWS S3, MediaConvert & CloudFront Pipeline |
| [ADR-006_PAYMENT_GATEWAY_ABSTRACTION.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/adr/ADR-006_PAYMENT_GATEWAY_ABSTRACTION.md) | Architecture Decision Record: Payment Gateway Abstraction & Webhook Verification |
| [ADR-007_MULTI_TENANT_ISOLATION.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/adr/ADR-007_MULTI_TENANT_ISOLATION.md) | Architecture Decision Record: Multi-Tenant Logical Partitioning |
| [ADR-008_ENVIRONMENT_AND_DEPLOYMENT_STRATEGY.md](file:///Applications/My%20Mac/OTT/Android%20-%20OTT/docs/architecture/adr/ADR-008_ENVIRONMENT_AND_DEPLOYMENT_STRATEGY.md) | Architecture Decision Record: Multi-Environment Isolation & Deployment Strategy |

---

## 9. Final Phase 2 Verdict

### Verdict: **PARTIAL — ARCHITECTURE PROPOSED, DECISIONS OUTSTANDING**

**Rationale:**
- The architecture, capabilities, data models, API contracts, threat model, cost equations, and phased roadmap are comprehensively documented and supported by reproducible evidence from the existing codebase.
- Zero code modifications or premature scaffolding were introduced during this specification phase.
- The verdict is properly designated as **PARTIAL** because critical architectural decisions (payment provider selection, backend repository migration, cost parameter confirmation, and AWS/Firebase operator credentials) require formal human sign-off before Phase 3 implementation can proceed.

---

## 10. Recommended Next Phase

Upon operator approval of this architecture specification and ADR-003, the project will be ready to proceed to:

**PHASE 3 — BACKEND REPOSITORY MIGRATION & API CONTRACT STABILIZATION**
- Migrate Cloud Functions, Firestore rules, and configuration from `Android - OTT/functions` into `Backend/` (`ott-backend.git`).
- Model and test the Series/Season/Episode data schema.
- Establish backend CI/CD pipeline and resolve non-breaking dependencies.
