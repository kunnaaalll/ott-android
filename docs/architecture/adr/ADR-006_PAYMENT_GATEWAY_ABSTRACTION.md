# ADR-006: Payment Gateway Abstraction and Raw-Body Webhook Verification

- **Status:** PROPOSED (Architectural Decision & Specification)
- **Date:** 2026-10-16
- **Authors:** Principal Application Security Architect, Backend Lead

---

## 1. Context and Problem Statement

The platform is subscription-only. Users must purchase a recurring subscription plan to access protected streaming content.
Currently:
- The backend has a generic `paymentWebhook` in `functions/src/index.ts` expecting an `x-gateway-signature` header and performing HMAC-SHA256 signature verification over `req.rawBody` using `crypto.timingSafeEqual`.
- The Android app has a generic `PaymentGateway` interface and `ExternalPaymentGatewayAdapter` in `core/payment/`.
- The specific production payment gateway (e.g., Stripe, Razorpay, Lemon Squeezy, Apple In-App Purchase, Google Play Billing) has not been finalized by the product team.

We must define an architectural contract that supports provider-specific webhook verification while preventing webhook forgery, replay attacks, cross-tenant subscription poisoning, and timing attacks.

---

## 2. Options Considered

### Option 1: Modular Gateway Adapter Interface with Strict Raw-Body Webhook Routing
Define a unified internal subscription state machine (`SubscriptionStatus`: `PENDING`, `ACTIVE`, `CANCELLED`, `EXPIRED`, `FAILED`) and implement dedicated gateway driver modules (e.g., `StripeGatewayAdapter`, `RazorpayGatewayAdapter`) behind a standard interface.
- Expose provider-specific endpoints (e.g., `/webhooks/stripe`, `/webhooks/razorpay`) or route dynamically based on header/payload signatures.
- Enforce strict raw-body capture before JSON parsing for cryptographic signature verification.
- Enforce atomic Firestore transaction idempotency using the gateway event ID in `/webhook_events/{eventId}`.
- Enforce cross-tenant metadata validation: `event.tenantId === user.tenantId`.

### Option 2: Hardcode Single Provider Directly into `index.ts`
Directly bind all webhook logic to one specific SDK without an abstraction layer.
- **Cons:** If business requirements demand changing gateways (e.g., expanding from India/Razorpay to US/Stripe, or adding Google Play Billing), core backend logic must be rewritten.

---

## 3. Decision and Recommendation

**Adopt Option 1: Modular Gateway Adapter Interface with Strict Raw-Body Webhook Routing.**

### Core Security & Verification Requirements:
1. **Raw-Body Preservation:** Webhook HTTP triggers must preserve the exact raw unparsed request buffer (`req.rawBody`). Parsing the request as JSON prior to HMAC verification alters whitespace and key order, invalidating cryptographic signatures.
2. **Timing-Safe Equality:** Signatures must always be compared using `crypto.timingSafeEqual()` over fixed-length buffer digests to prevent side-channel timing attacks.
3. **Fail-Closed Configuration:** If `GATEWAY_WEBHOOK_SECRET` is missing or blank, the endpoint must reject all requests with HTTP 401.
4. **Idempotent Transaction:** Webhook processing must use a Firestore transaction on `/webhook_events/{eventId}`. If `doc.exists`, processing is aborted immediately with HTTP 200 to prevent duplicate credit grants on network retries.
5. **Cross-Tenant Guard:** The backend must retrieve the target user document (`/users/{userId}`) and verify that `userDoc.data().tenantId == event.metadata.tenantId`. Webhooks with mismatched or missing tenant metadata are rejected and logged.
6. **Server-Authoritative Subscriptions:** `firestore.rules` enforces `allow write: if false` on `/subscriptions/{id}`. Only backend Admin SDK can write subscription activations.

---

## 4. Consequences and Action Items for Implementation

- In Phase 5, the operator/product team will select the primary payment provider (e.g., Stripe or Razorpay).
- The adapter will parse provider-specific headers (e.g., `Stripe-Signature` or `X-Razorpay-Signature`) and payload schemas into the internal canonical `SubscriptionEvent`.
- Webhook signing secrets will be provisioned in Firebase Secret Manager (`firebase functions:secrets:set PAYMENT_WEBHOOK_SECRET`).

---

## 5. Evidence and Approval Status

- **Evidence:** `functions/src/index.ts:30-195` (contains existing verified prototype of raw-body HMAC, timingSafeEqual, and cross-tenant checks).
- **Approval Status:** **RECOMMENDED — PROVIDER SELECTION PENDING OPERATOR DECISION**.
