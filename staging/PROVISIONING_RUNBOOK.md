# Staging Provisioning Runbook

**WARNING: Do not run these commands until explicit operator authorization is granted.**

This runbook describes the process for deploying the staging environment from scratch.

## 1. Prerequisites
*   AWS CLI installed and authenticated with an account authorized for staging.
*   Terraform installed (`>= 1.5.0`).
*   Firebase CLI installed and authenticated (`firebase login`).
*   A newly created, dedicated Firebase Project for staging.
*   CloudFront Key Pair generated (see `SECRETS_INVENTORY.md`).

## 2. Infrastructure Deployment (AWS)

1.  Navigate to the staging infrastructure directory:
    ```bash
    cd staging
    ```
2.  Copy the example variables file:
    ```bash
    cp terraform.tfvars.example terraform.tfvars
    ```
3.  Edit `terraform.tfvars` with appropriate values, particularly the `cloudfront_public_key_pem`.
4.  Initialize Terraform:
    ```bash
    terraform init
    ```
5.  Review the planned changes:
    ```bash
    terraform plan
    ```
6.  Apply the changes (Requires Operator Approval):
    ```bash
    terraform apply
    ```
7.  Capture the outputs (S3 Bucket Name, CloudFront Domain, IAM Roles).

## 3. Firebase Configuration

1.  Ensure you are using the correct project:
    ```bash
    firebase use <STAGING_PROJECT_ID>
    ```
2.  Set all required secrets in Secret Manager (see `SECRETS_INVENTORY.md`):
    ```bash
    firebase functions:secrets:set CLOUDFRONT_PRIVATE_KEY
    firebase functions:secrets:set CLOUDFRONT_KEY_PAIR_ID
    firebase functions:secrets:set CLOUDFRONT_DOMAIN
    # ... set remaining secrets
    ```
3.  Deploy Firestore Security Rules:
    ```bash
    firebase deploy --only firestore:rules
    ```

## 4. Backend Deployment

1.  Build and deploy Firebase Functions:
    ```bash
    cd functions
    npm ci
    npm run build
    firebase deploy --only functions
    ```

## 5. Android Client Configuration

1.  Update `app/build.gradle.kts` to point to the staging CloudFront domain:
    ```kotlin
    buildConfigField("String", "CLOUDFRONT_HOSTNAME", "\"<STAGING_CLOUDFRONT_DOMAIN>\"")
    ```
2.  Build the staging APK:
    ```bash
    ./gradlew assembleDebug
    ```
