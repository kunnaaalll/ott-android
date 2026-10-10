# Secrets Inventory and Setup Guide

This document catalogs the required configuration values and secrets for the OTT staging environment.

## Secrets Inventory

The following values are highly sensitive. **Never commit them to source control.**

| Secret Name | Description | Source | Where it goes |
| --- | --- | --- | --- |
| `CLOUDFRONT_PRIVATE_KEY` | PEM-encoded RSA-2048 private key used to sign CloudFront cookies. | Generated locally (e.g. via `openssl`). | Firebase Secret Manager. |
| `GATEWAY_WEBHOOK_SECRET` | Signing secret used to verify payment gateway webhooks. | Payment Provider Dashboard. | Firebase Secret Manager. |
| `FIREBASE_TOKEN` | CI/CD deployment token for staging Firebase Functions. | Firebase CLI (`firebase login:ci`). | GitHub Repository Secrets. |

## Configuration Values (Non-Secret)

The following values are configuration identifiers. While not strictly "secret" (they do not provide access on their own), they should be managed carefully.

| Variable Name | Description | Source | Where it goes |
| --- | --- | --- | --- |
| `CLOUDFRONT_KEY_PAIR_ID` | Identifier for the CloudFront Public Key used for signed cookies. | AWS Console (CloudFront). | Firebase Secret Manager. |
| `CLOUDFRONT_DOMAIN` | Domain name of the CloudFront distribution (e.g., `d123.cloudfront.net`). | AWS Console (CloudFront). | Firebase Secret Manager & Android `BuildConfig`. |
| `AWS_REGION` | Target AWS region (e.g., `us-east-1`). | Infrastructure Decision. | Firebase Secret Manager. |
| `S3_UPLOAD_BUCKET` | The name of the provisioned S3 content bucket. | Terraform Output. | Firebase Secret Manager. |
| `MEDIACONVERT_ENDPOINT` | Account-specific AWS Elemental MediaConvert endpoint URL. | AWS Console (MediaConvert). | Firebase Secret Manager. |
| `MEDIACONVERT_ROLE` | ARN of the IAM role assumed by MediaConvert to read/write S3. | Terraform Output. | Firebase Secret Manager. |
| `MEDIACONVERT_SNS_TOPIC_ARN`| ARN of the SNS topic used for MediaConvert webhook notifications. | Terraform Output. | Firebase Secret Manager. |

## Setup Guide

1. **Generate the CloudFront Key Pair:**
   ```bash
   openssl genrsa -out private_key.pem 2048
   openssl rsa -pubout -in private_key.pem -out public_key.pem
   ```
2. **Apply Terraform (See Provisioning Runbook):**
   Use `public_key.pem` as the value for `cloudfront_public_key_pem`.
3. **Configure Firebase Secrets:**
   ```bash
   firebase functions:secrets:set CLOUDFRONT_PRIVATE_KEY --project <STAGING_PROJECT_ID>
   # Provide the contents of private_key.pem (replace literal newlines with \n or enter directly via CLI prompt)
   
   firebase functions:secrets:set CLOUDFRONT_KEY_PAIR_ID --project <STAGING_PROJECT_ID>
   # And repeat for all other secrets and configurations.
   ```
