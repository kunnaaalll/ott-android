# Cost and Resource Inventory (Staging)

This document estimates the potential ongoing and usage-based costs for the staging environment. 
**Note:** All prices are estimates based on standard `us-east-1` pricing as of 2026.

## Potentially Billable Components

### AWS S3
*   **Resource:** `aws_s3_bucket.ott_content`
*   **Cost Metric:** Storage (GB/month) and Data Transfer Out.
*   **Estimate:** ~$0.023/GB/month. Staging usage is typically low (< 50GB), expecting < $2.00/month.

### AWS CloudFront
*   **Resource:** `aws_cloudfront_distribution.ott_distribution`
*   **Cost Metric:** Data Transfer Out to Internet and HTTP/HTTPS Requests.
*   **Estimate:** ~$0.085/GB. For staging, usage depends on testing volume. Assuming 100GB/month of streaming testing: ~$8.50/month.

### AWS Elemental MediaConvert
*   **Resource:** Automated Transcoding Jobs.
*   **Cost Metric:** Per-minute output (varies by resolution and codec).
*   **Estimate:** ~$0.015/minute for basic HD tiers. Highly variable based on testing frequency. E.g., 10 hours of video transcoded = ~$9.00.

### AWS CloudWatch / EventBridge / SNS
*   **Resources:** SNS Topics, Rules.
*   **Cost Metric:** Events processed / Messages published.
*   **Estimate:** Free tier typically covers staging volumes. Expect $0 unless stress testing.

### Firebase (GCP)
*   **Resources:** Cloud Functions, Firestore, Secret Manager.
*   **Cost Metric:** Function Invocations, Firestore Reads/Writes, Secret versions.
*   **Estimate:** Firebase Blaze Plan required. Staging volumes usually fall well within the free tier limits (2M function invocations, 50K Firestore reads/day).

## Summary
A moderately utilized staging environment is expected to cost between **$5 and $25 per month**.
To minimize costs, ensure S3 lifecycle policies are active (already defined in Terraform) to prune old uploads.
