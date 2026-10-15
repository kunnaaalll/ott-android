# ADR-002: Serverless Backend Runtime (Firebase Cloud Functions vs Dedicated Container Service)

- **Status:** PROPOSED (Architectural Recommendation)
- **Date:** 2026-10-16
- **Authors:** Principal Backend Architect, Cloud Infrastructure Architect

---

## 1. Context and Problem Statement

The platform requires a secure backend to enforce business logic:
- Verifying subscriber entitlements and issuing CloudFront signed cookies for protected HLS video.
- Managing secure S3 upload pre-signatures and validating file size/type constraints.
- Triggering AWS MediaConvert transcoding jobs and processing SNS state callbacks.
- Receiving and verifying third-party payment gateway webhooks.
- Enforcing multi-tenant isolation and administrative role boundaries.

We must decide whether to continue using serverless Firebase Cloud Functions (Node.js runtime) or introduce a dedicated backend service (e.g., Express/NestJS running on AWS ECS, EC2, Cloud Run, or Kubernetes).

---

## 2. Options Considered

### Option 1: Serverless Firebase Cloud Functions (Node.js 18 / 20)
- Event-driven, auto-scaling serverless runtime directly integrated with Firebase Authentication and Cloud Firestore.
- Currently has 35 passing unit tests covering media upload, playback authorization, and payment webhooks in `functions/src/`.
- Zero baseline cost when idle.

### Option 2: Dedicated Container Service (ECS / Cloud Run / Kubernetes / VPS)
- Monolithic or modular Express/Fastify/NestJS service running 24/7 in Docker containers.
- Requires container registries, load balancers, VPC networking, auto-scaling groups, SSL termination, and continuous monitoring.

---

## 3. Decision and Recommendation

**Retain and Standardize on Option 1: Serverless Firebase Cloud Functions.**

### Rationale:
1. **Adherence to Core Principles:** Principle 2.1 in `MASTER.md` explicitly mandates: *"No VPS, no dedicated application servers, no Nginx, no Kubernetes. Use managed/serverless cloud instead."*
2. **Cost-Conscious Architecture:** A streaming platform startup has variable load patterns. Cloud Functions cost $0.00 at idle, whereas managed container clusters (ECS Fargate + ALB) incur a minimum baseline cost of $35–$120/month regardless of traffic.
3. **Existing Verified Implementation:** The existing codebase in `functions/` is already written in TypeScript, strictly enforces path traversal guards, signed cookies, timing-safe webhook HMAC verification, and passes 35/35 unit tests.
4. **Seamless Identity Integration:** Cloud Functions automatically unpack and verify Firebase ID tokens in `context.auth` without custom JWT middleware.

---

## 4. Consequences and Trade-offs

### Positive:
- True zero-idle operating cost.
- Instant auto-scaling from 0 to thousands of requests.
- No OS patching, VPC maintenance, or container orchestration toil.
- Native integration with Firestore Admin SDK and Firebase Secret Manager.

### Negative / Risks:
- Cold starts: Node.js runtime cold starts typically take 200–800ms. Can be mitigated using `minInstances = 1` for critical endpoints (`authorizePlayback`) or migrating to 2nd Gen Cloud Functions if latency becomes an issue.
- Execution timeout: Maximum timeout is 9 minutes (HTTP) or 60 minutes (2nd Gen background). MediaConvert handles heavy media processing asynchronously, so Functions only trigger jobs and handle callbacks (execution time < 2s).

---

## 5. Evidence and Approval Status

- **Evidence:** `functions/src/index.ts`, `functions/src/media.ts`, `functions/src/playback.ts` (35 passing unit tests).
- **Approval Status:** **RECOMMENDED — PENDING OPERATOR SIGN-OFF**.
