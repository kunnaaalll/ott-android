# ADR-005: AWS S3, MediaConvert, and CloudFront for Protected HLS Streaming

- **Status:** PROPOSED (Established Direction)
- **Date:** 2026-10-16
- **Authors:** Principal Cloud Architect, Media Systems Lead

---

## 1. Context and Problem Statement

A subscription-only OTT platform requires:
- High-capacity, durable, private storage for mezzanine/source master videos and transcoded HLS stream packages.
- Broadcast-quality automated video transcoding (generating multi-bitrate HLS playlists `.m3u8` and media segments `.ts` / `.m4s`).
- Global low-latency CDN content distribution.
- Cryptographically enforced access protection so unauthorized users, expired subscribers, and non-paying clients cannot download or play premium video files.
- Separation of concerns between public trailer previews and protected main content.

We must decide the media storage, processing, and delivery architecture.

---

## 2. Options Considered

### Option 1: AWS Media Stack (S3 + AWS Elemental MediaConvert + Amazon CloudFront with Signed Cookies)
- **Storage:** Amazon S3 with Block Public Access enabled, KMS/SSE-S3 encryption, and automated lifecycle policies.
- **Transcoding:** AWS Elemental MediaConvert (QVBR rate-control, H.264/AAC, Apple HLS packaging).
- **Delivery:** Amazon CloudFront with Origin Access Control (OAC), custom error caching, and Signed Cookies (`CloudFront-Policy`, `CloudFront-Signature`, `CloudFront-Key-Pair-Id`).
- **Authorization:** Firebase Cloud Function (`authorizePlayback`) verifies subscription and issues 15-minute cryptographically signed cookies scoped strictly to the content directory.

### Option 2: Third-Party Managed Video SaaS (Mux, Cloudflare Stream, Vimeo OTT)
- All-in-one hosted video platforms with black-box transcoding and players.
- **Pros:** Fast initial setup.
- **Cons:** Significantly higher per-minute streaming and encoding fees at scale; vendor lock-in; less control over raw media master assets; less flexible multi-tenant folder isolation.

### Option 3: Self-Hosted FFmpeg on Cloud VMs
- Running FFmpeg worker processes on EC2 instances or Docker containers.
- **Pros:** Direct control over FFmpeg flags.
- **Cons:** High operational toil; maintaining server fleets, queue workers, disk volume auto-scaling, and cluster failover violates Principle 2.1.

---

## 3. Decision and Recommendation

**Retain and Standardize on Option 1: AWS S3 + Elemental MediaConvert + CloudFront with Signed Cookies.**

### Rationale:
1. **Industry-Standard Security:** CloudFront signed cookies allow seamless native HLS playback (sub-requests for `.m3u8` variant playlists and individual `.ts`/`.m4s` segments inherit the cookie) without appending query-string tokens to every segment URL.
2. **Strict Cryptographic Scoping:** Cookies are signed with an RSA-2048 private key and scoped strictly to `content/{tenantId}/{contentId}/{main|trailer}/hls/*`. A trailer cookie cannot access main content, and a user cannot access another tenant's assets.
3. **Existing Validated Infrastructure as Code:** `staging/main.tf` is fully authored in OpenTofu/Terraform and syntax-validated (`tofu validate` passes). It provisions the S3 bucket, CloudFront distribution, OAC, Key Group, MediaConvert IAM role, and EventBridge/SNS notification rules.
4. **Tested Backend Integration:** `functions/src/media.ts` and `playback.ts` are already implemented and pass 35 unit tests validating MediaConvert job parameters, SNS signatures, and CloudFront cookie generation.

---

## 4. Consequences and Operational Requirements

### Positive:
- Highly scalable, serverless video pipeline with no infrastructure maintenance.
- Predictable AWS pricing with CloudFront edge caching reducing S3 egress costs.
- Mezzanine files automatically expire after 30 days via S3 lifecycle rules.

### Negative / Prerequisites:
- Staging and production AWS accounts required.
- Operator must generate an RSA-2048 key pair, configure `CLOUDFRONT_PUBLIC_KEY_PEM` in OpenTofu, and store `CLOUDFRONT_PRIVATE_KEY` and `CLOUDFRONT_KEY_PAIR_ID` in Firebase Secret Manager.
- MediaConvert requires account-specific regional endpoints.

---

## 5. Evidence and Approval Status

- **Evidence:** `staging/main.tf`, `functions/src/media.ts`, `functions/src/playback.ts`, `AWS_INFRASTRUCTURE.md`.
- **Approval Status:** **ESTABLISHED DIRECTION — RECONFIRMED**.
