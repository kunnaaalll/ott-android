# OTT Platform — AWS Infrastructure Configuration Guide

> **Status:** Infrastructure as Code (Manual / CDK-Ready)
> This document describes the required AWS resources and their configuration.
> No actual AWS credentials are stored here.

---

## 1. Required AWS Resources

```
S3 Bucket (private — premium video storage)
    └── Block Public Access: ALL ON
    └── Object Ownership: BucketOwnerEnforced
    └── Encryption: SSE-S3 (AES-256) or SSE-KMS if compliance requires it
    └── Versioning: Enabled (protects against accidental deletion)
    └── Lifecycle:
        - uploads/ prefix: expire source files after 30 days post-processing
        - content/ prefix: no expiry (playback assets)

CloudFront Distribution
    └── Origin: Private S3 via OAC (Origin Access Control)
    └── Viewer Protocol: Redirect HTTP → HTTPS
    └── Allowed Methods: GET, HEAD only
    └── Trusted Key Group: <key-group-id> (contains the public key matching CLOUDFRONT_PRIVATE_KEY)
    └── Cache Behavior: separate behaviors for m3u8 (short TTL) vs segments (longer TTL)
    └── Default Root Object: none (no directory listing)
    └── Custom Error Pages: 403 → custom error (do not reveal S3 error details)
    └── WAF: Optional — recommended for rate limiting at edge

CloudFront Public Key
    └── Upload: RSA-2048 public key matching the private key in Firebase Secrets
    └── Key Group: Attach this key to a Key Group
    └── Distribution: Attach the Key Group to the CloudFront distribution's default behavior

MediaConvert
    └── IAM Role: MediaConvertRole
        - s3:GetObject on ott-content-bucket/uploads/*
        - s3:PutObject on ott-content-bucket/content/*
    └── Endpoint: Get per-account endpoint from AWS console
    └── Queue: Default or dedicated queue

SNS Topic (MediaConvert Events → Firebase Function)
    └── EventBridge Rule: aws.mediaconvert → SNS topic
    └── SNS Subscription: HTTPS endpoint → mediaConvertWebhook Cloud Function URL
    └── SNS message signing: Verified by sns-validator library
```

---

## 2. S3 Bucket Policy (Template)

Replace `{DISTRIBUTION_ARN}` and `{BUCKET_NAME}` with actual values.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontOACReadOnly",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudfront.amazonaws.com"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::{BUCKET_NAME}/content/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "{DISTRIBUTION_ARN}"
        }
      }
    },
    {
      "Sid": "DenyPublicAccess",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::{BUCKET_NAME}/*",
      "Condition": {
        "StringNotEquals": {
          "AWS:SourceArn": "{DISTRIBUTION_ARN}"
        }
      }
    }
  ]
}
```

> **Critical:** The `DenyPublicAccess` statement ensures that even if Block Public Access
> is accidentally disabled, no direct S3 access is possible without a valid OAC request
> from the authorized CloudFront distribution.

---

## 3. CloudFront Cache Behaviors

| Path Pattern | Cache TTL | Cookie Forwarding | Notes |
|---|---|---|---|
| `content/*/main/hls/*.m3u8` | 30s (master), 5s (variant) | CloudFront-* signed cookies | Short TTL — playlists change |
| `content/*/main/hls/*.ts` | 86400s (1 day) | CloudFront-* signed cookies | Segments are immutable |
| `content/*/main/hls/*.m4s` | 86400s (1 day) | CloudFront-* signed cookies | fMP4 segments |
| `content/*/trailer/hls/*` | 300s | CloudFront-* signed cookies | Trailer content |
| Default | No cache | None | Catch-all deny |

> **Authorization vs Caching:** CloudFront evaluates signed cookie authorization for
> **every request**, including cached responses. A cached segment is still protected —
> CloudFront checks the cookie before serving from cache.

---

## 4. MIME Type Configuration

Ensure correct Content-Type headers for HLS playback:

| Extension | Content-Type |
|---|---|
| `.m3u8` | `application/vnd.apple.mpegurl` |
| `.ts` | `video/MP2T` |
| `.m4s` | `video/iso.segment` |
| `.mp4` | `video/mp4` |
| `.key` | `application/octet-stream` |

Configure via CloudFront response headers policy or S3 object metadata at upload time.

---

## 5. Firebase Secret Configuration

Set the following secrets using Firebase CLI (never commit these values):

```bash
# CloudFront signing key (PEM format — escape newlines as \n for Firebase Secrets)
firebase functions:secrets:set CLOUDFRONT_PRIVATE_KEY

# CloudFront public key identifier (from AWS console — NOT the AWS access key ID)
firebase functions:secrets:set CLOUDFRONT_KEY_PAIR_ID

# CloudFront distribution domain (e.g., d1234abcde.cloudfront.net)
firebase functions:secrets:set CLOUDFRONT_DOMAIN

# AWS region for S3 and MediaConvert
firebase functions:secrets:set AWS_REGION

# S3 bucket name containing the video content
firebase functions:secrets:set S3_UPLOAD_BUCKET

# MediaConvert per-account endpoint URL
firebase functions:secrets:set MEDIACONVERT_ENDPOINT

# MediaConvert IAM role ARN
firebase functions:secrets:set MEDIACONVERT_ROLE

# Payment gateway webhook signing secret
firebase functions:secrets:set GATEWAY_WEBHOOK_SECRET
```

Bind secrets to functions in `firebase.json`:

```json
{
  "functions": {
    "source": "functions",
    "runtime": "nodejs18",
    "secretEnvironmentVariables": [
      { "key": "CLOUDFRONT_PRIVATE_KEY" },
      { "key": "CLOUDFRONT_KEY_PAIR_ID" },
      { "key": "CLOUDFRONT_DOMAIN" },
      { "key": "AWS_REGION" },
      { "key": "S3_UPLOAD_BUCKET" },
      { "key": "MEDIACONVERT_ENDPOINT" },
      { "key": "MEDIACONVERT_ROLE" },
      { "key": "GATEWAY_WEBHOOK_SECRET" }
    ]
  }
}
```

---

## 6. IAM Role — Firebase Functions Execution Role

The Firebase Functions service account needs:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3PresignedUploadURLs",
      "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": "arn:aws:s3:::{BUCKET_NAME}/uploads/*"
    },
    {
      "Sid": "S3VerifyUpload",
      "Effect": "Allow",
      "Action": ["s3:HeadObject"],
      "Resource": "arn:aws:s3:::{BUCKET_NAME}/uploads/*"
    },
    {
      "Sid": "MediaConvertJobs",
      "Effect": "Allow",
      "Action": ["mediaconvert:CreateJob", "mediaconvert:GetJob"],
      "Resource": "*"
    },
    {
      "Sid": "MediaConvertPassRole",
      "Effect": "Allow",
      "Action": ["iam:PassRole"],
      "Resource": "arn:aws:iam::{ACCOUNT_ID}:role/MediaConvertRole"
    }
  ]
}
```

> **No S3 read permission for the Functions execution role** — functions generate
> presigned URLs using PutObject and verify existence with HeadObject only.
> CloudFront reads S3 via OAC service principal, not via function credentials.

---

## 7. HLS Output Path Structure

MediaConvert writes to:

```
s3://{BUCKET}/content/{tenantId}/{contentId}/main/hls/     ← main content
s3://{BUCKET}/content/{tenantId}/{contentId}/trailer/hls/  ← trailer
```

Source uploads arrive at:

```
s3://{BUCKET}/uploads/{tenantId}/{contentId}/source/{mediaType}/{uploadId}
```

Images (posters, backdrops) are stored at:

```
s3://{BUCKET}/uploads/{tenantId}/{contentId}/source/poster/{uploadId}
s3://{BUCKET}/uploads/{tenantId}/{contentId}/source/backdrop/{uploadId}
```

---

## 8. CloudFront Signed Cookie Scope

| Request Type | Signed Cookie Resource URL |
|---|---|
| Main content | `https://{domain}/content/{tenantId}/{contentId}/main/hls/*` |
| Trailer | `https://{domain}/content/{tenantId}/{contentId}/trailer/hls/*` |

**A trailer cookie cannot access main content.**
**Main content cookies are scoped to a single content item — not the entire tenant.**

---

## 9. Negative Security Tests (Manual)

Run these tests against the deployed infrastructure before production:

| Test | Command | Expected |
|---|---|---|
| Direct S3 object access | `curl https://{bucket}.s3.{region}.amazonaws.com/content/{key}` | 403 Forbidden |
| CloudFront without cookies | `curl https://{domain}/content/{tenantId}/{contentId}/main/hls/master.m3u8` | 403 Forbidden |
| CloudFront with expired cookies | Set `CloudFront-Policy` with past expiry | 403 Forbidden |
| Trailer cookies for main content | Use trailer-scoped cookies on main path | 403 Forbidden |
| Arbitrary path | `curl https://{domain}/content/{tenantId}/{contentId}/../../other/path` | 403 Forbidden |

---

## 10. Cost Model (Estimates)

> All estimates assume `us-east-1` and standard pricing as of 2026.

### Low-traffic (testing / <100 users)
- S3 storage: ~$0.023/GB/month for video library
- CloudFront: $0.0085/GB data transfer out + $0.0075/10k HTTPS requests
- MediaConvert: ~$0.015/minute of output video (HD)
- Firebase Functions: Likely within free tier (2M invocations/month)
- **Estimated total: $5–$30/month for small libraries**

### Small production (<1,000 active subscribers, 10TB/month streaming)
- S3: ~$5–20/month for library
- CloudFront: ~$85/month for 10TB transfer + requests
- MediaConvert: ~$15–50/month depending on content volume
- Firebase: ~$25–100/month depending on reads/writes
- **Estimated total: $130–$270/month**

### Higher-concurrency (10,000+ concurrent streams)
- CloudFront: Primary cost driver at scale — ~$850/month per 100TB
- Consider reserved capacity pricing and CloudFront savings plans
- MediaConvert: One-time processing cost, not streaming cost
- **Primary cost drivers: CloudFront data transfer + S3 storage**

> Enable CloudFront cost alerts and S3 storage cost thresholds in AWS Cost Explorer.
