variable "aws_region" {
  type        = string
  description = "AWS region for staging resources"
  default     = "us-east-1"
}

variable "environment" {
  type        = string
  description = "Environment name (e.g., staging)"
  default     = "staging"
}

variable "content_bucket_name" {
  type        = string
  description = "Name of the S3 bucket to create for OTT content. Must be globally unique."
}

variable "cloudfront_public_key_pem" {
  type        = string
  description = "PEM-encoded public key for validating CloudFront signed cookies"
  sensitive   = true
}
