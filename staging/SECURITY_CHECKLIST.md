# Staging Security Verification Checklist

Execute this checklist manually against the newly provisioned staging environment to confirm security properties.

## S3 & CloudFront Controls
- [ ] **Direct S3 Access Denied:** Verify `curl -I https://<BUCKET_NAME>.s3.<REGION>.amazonaws.com/content/test.m3u8` returns `403 Forbidden`.
- [ ] **CloudFront Anonymous Denied:** Verify `curl -I https://<CLOUDFRONT_DOMAIN>/content/tenant-a/content-1/main/hls/master.m3u8` returns `403 Forbidden`.
- [ ] **CloudFront Valid Signed Cookies Allowed:** Verify the same URL with valid signed cookies returns `200 OK`.
- [ ] **CloudFront Expired Cookies Denied:** Verify cookies with a past `dateLessThan` return `403 Forbidden`.
- [ ] **Cross-Content Isolation:** Verify a cookie signed for `content-1` cannot access `content-2` assets.
- [ ] **Trailer to Main Content Isolation:** Verify a cookie signed for `content-1/trailer` cannot access `content-1/main` assets.
- [ ] **HTTPS Enforced:** Verify HTTP requests redirect to HTTPS or are rejected.

## Firebase & Authorization
- [ ] **Rate Limiting:** Execute 21 concurrent playback authorization requests for the same user. Ensure the 21st fails with `RESOURCE_EXHAUSTED`.
- [ ] **Tenant Isolation:** Ensure a user in `tenant-A` requesting content assigned to `tenant-B` receives `PERMISSION_DENIED`.
- [ ] **Subscription Enforcement:** Ensure a user with an expired subscription attempting to authorize main content receives `PERMISSION_DENIED`.
- [ ] **No Client-Side Overrides:** Verify that a client attempting to write to `_rateLimits` or `subscriptions` directly via Firestore API receives `PERMISSION_DENIED`.

## Secrets & Execution
- [ ] **Secrets Isolated:** Verify no sensitive credentials (e.g., `CLOUDFRONT_PRIVATE_KEY`) are exposed in Cloud Functions logs, client responses, or source control.
- [ ] **MediaConvert Least Privilege:** Verify the MediaConvert role cannot read or write outside the designated S3 prefixes (`uploads/` and `content/`).
