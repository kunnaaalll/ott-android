# ADR-008: Multi-Environment Isolation and Deployment Strategy

- **Status:** PROPOSED (Architectural Decision & Specification)
- **Date:** 2026-10-16
- **Authors:** Cloud Infrastructure Architect, Release Engineering Lead

---

## 1. Context and Problem Statement

The platform requires four distinct operational tiers:
1. **Local Development:** Offline developer testing and emulator validation.
2. **Automated Testing (CI):** Static analysis, compilation, unit tests, security scans.
3. **Staging:** A realistic, deployed cloud environment mirroring production for integration tests, media validation, and end-to-end user verification.
4. **Production:** Live environment serving real subscribers, handling payments, and storing customer data.

We must define strict boundaries between staging and production resources to prevent data corruption, accidental destructive operations, or security credential leaks. Furthermore, we must formalize the role of Phase 9 as the dedicated live-deployment verification phase.

---

## 2. Options Considered

### Option 1: Complete Project and Resource Boundary Isolation
- **Firebase:** Separate Firebase project for Staging (`ott-staging-XXXXX`) and Production (`ott-prod-XXXXX`).
- **AWS:** Separate S3 buckets (`ott-staging-content-bucket` vs `ott-prod-content-bucket`), distinct CloudFront distributions, separate key pairs, and separate MediaConvert queues/roles.
- **Payment Gateway:** Test mode keys (`sk_test_*`) and staging webhook endpoints vs live mode keys (`sk_live_*`).
- **Safety Preconditions:** IaC definitions enforce `force_destroy = true` strictly in staging via OpenTofu lifecycle preconditions; production buckets prohibit `force_destroy` and mandate MFA delete / versioning.
- **Phase Discipline:** Implementation phases (Phases 3–8) prepare, scaffold, test locally, and validate components. Full live-system acceptance and deployment verification is strictly reserved for Phase 9.

### Option 2: Shared Firebase/AWS Project with Prefix Partitioning
- Using a single Firebase project and AWS account, with staging documents prefixed by `staging_` or using separate collections.
- **Cons:** High risk of accidental production data deletion, shared quota exhaustion, credential leakage, and invalid compliance posture.

---

## 3. Decision and Recommendation

**Adopt Option 1: Complete Project and Resource Boundary Isolation with Phase 9 Live-Deployment Reserve.**

### Key Environment Standards:
1. **Local Development:** Uses Firebase Emulator Suite (`auth: 9099`, `functions: 5001`, `firestore: 8080`) configured via `firebase.json`. No live AWS or payment credentials needed for daily development.
2. **CI Pipeline:** Automatically executes linting, TypeScript build, unit tests, and security audits on pull requests. Runs headless without external credentials.
3. **Staging Environment:** Provisioned using `staging/main.tf` via OpenTofu. Guarded by S3 lifecycle rules and staging preconditions. Used for operator review, MediaConvert transcode verification, and web/mobile integration.
4. **Production Environment:** Independent OpenTofu workspace/module with production state locking, multi-region CloudFront delivery, and strict least-privilege IAM roles.
5. **Phase 9 Reservation:** Under no circumstances will live deployment or end-to-end production testing be considered completed in earlier phases. Phase 9 is explicitly reserved as the live-system acceptance gate.

---

## 4. Consequences and Mitigations

### Positive:
- Zero cross-contamination between test data and production financial records.
- Staging buckets can be safely emptied and destroyed without risking customer videos.
- Clear CI/CD promotion path with human review gates.

### Negative / Prerequisites:
- Requires the operator to supply two sets of AWS IAM credentials and create two Firebase projects.

---

## 5. Evidence and Approval Status

- **Evidence:** `staging/main.tf:19-26` (verified staging precondition on `force_destroy`), `firebase.json` (emulator port mappings).
- **Approval Status:** **ESTABLISHED DIRECTION — SPECIFICATION COMPLETE**.
