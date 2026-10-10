# Rollback and Cleanup Plan (Staging)

This document outlines the steps to tear down the staging environment and revert any application configuration.

## 1. Destroy AWS Infrastructure
To completely remove all AWS resources created for staging:

1. Navigate to the staging infrastructure directory and execute destroy:
   ```bash
   cd staging
   terraform destroy
   ```
3. Confirm the destruction plan when prompted.

## 2. Revert Application Configuration
If rolling back staging changes from the codebase:

1. **Android:** Revert `app/build.gradle.kts` to remove the staging CloudFront hostname from `CLOUDFRONT_HOSTNAME`.
2. **Firebase Secrets:** Destroy staging secrets in the Firebase project:
   ```bash
   firebase functions:secrets:destroy CLOUDFRONT_PRIVATE_KEY
   firebase functions:secrets:destroy CLOUDFRONT_KEY_PAIR_ID
   firebase functions:secrets:destroy CLOUDFRONT_DOMAIN
   # Repeat for all other secrets...
   ```

## 3. Delete Firebase Staging Project (Optional)
If the staging environment is no longer needed at all, the entire Firebase project can be shut down via the Google Cloud Console. This automatically destroys the Firestore database, Auth users, and deployed Cloud Functions.
