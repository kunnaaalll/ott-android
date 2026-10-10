output "s3_bucket_name" {
  value       = aws_s3_bucket.ott_content.id
  description = "The name of the provisioned S3 bucket"
}

output "cloudfront_distribution_domain" {
  value       = aws_cloudfront_distribution.ott_distribution.domain_name
  description = "The domain name of the CloudFront distribution"
}

output "cloudfront_key_pair_id" {
  value       = aws_cloudfront_public_key.auth_key.id
  description = "The Key Pair ID for CloudFront signed cookies"
}

output "mediaconvert_sns_topic_arn" {
  value       = aws_sns_topic.mediaconvert_events.arn
  description = "SNS Topic ARN for MediaConvert events"
}

output "mediaconvert_role_arn" {
  value       = aws_iam_role.mediaconvert_role.arn
  description = "IAM Role ARN for MediaConvert"
}
