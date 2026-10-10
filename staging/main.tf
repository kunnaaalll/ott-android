terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# -----------------------------------------------------------------------------
# S3 Bucket Configuration
# -----------------------------------------------------------------------------
resource "aws_s3_bucket" "ott_content" {
  bucket        = var.content_bucket_name
  force_destroy = true

  lifecycle {
    precondition {
      condition     = var.environment == "staging" && length(regexall("(?i)staging", var.content_bucket_name)) > 0
      error_message = "force_destroy is enabled, which is dangerous. This configuration is restricted to the staging environment."
    }
  }
}

resource "aws_s3_bucket_public_access_block" "ott_content_block" {
  bucket = aws_s3_bucket.ott_content.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "ott_content_ownership" {
  bucket = aws_s3_bucket.ott_content.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_versioning" "ott_content_versioning" {
  bucket = aws_s3_bucket.ott_content.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "ott_content_lifecycle" {
  bucket = aws_s3_bucket.ott_content.id

  rule {
    id     = "expire_uploads"
    status = "Enabled"

    filter {
      prefix = "uploads/"
    }
    expiration {
      days = 30
    }
    noncurrent_version_expiration {
      noncurrent_days = 7
    }
    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}

# -----------------------------------------------------------------------------
# CloudFront Origin Access Control
# -----------------------------------------------------------------------------
resource "aws_cloudfront_origin_access_control" "oac" {
  name                              = "${var.environment}-oac"
  description                       = "OAC for OTT Content Bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# Bucket policy allowing CloudFront to access the content prefix
resource "aws_s3_bucket_policy" "ott_content_policy" {
  bucket = aws_s3_bucket.ott_content.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowCloudFrontOACReadOnly"
        Effect    = "Allow"
        Principal = { Service = "cloudfront.amazonaws.com" }
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.ott_content.arn}/content/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.ott_distribution.arn
          }
        }
      },
      {
        Sid       = "DenyPublicAccess"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.ott_content.arn}/*"
        Condition = {
          StringNotEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.ott_distribution.arn
          }
        }
      }
    ]
  })
}

# -----------------------------------------------------------------------------
# CloudFront Key Group & Public Key
# -----------------------------------------------------------------------------
resource "aws_cloudfront_public_key" "auth_key" {
  name        = "${var.environment}-auth-key"
  encoded_key = var.cloudfront_public_key_pem
  comment     = "Public key for signed cookies"
}

resource "aws_cloudfront_key_group" "auth_group" {
  name    = "${var.environment}-auth-group"
  items   = [aws_cloudfront_public_key.auth_key.id]
  comment = "Key group for signed cookies"
}

# -----------------------------------------------------------------------------
# CloudFront Distribution
# -----------------------------------------------------------------------------
resource "aws_cloudfront_distribution" "ott_distribution" {
  enabled             = true
  is_ipv6_enabled     = true
  wait_for_deployment = false
  # Do not define a default_root_object to prevent directory listing

  origin {
    domain_name              = aws_s3_bucket.ott_content.bucket_regional_domain_name
    origin_id                = "S3-${aws_s3_bucket.ott_content.id}"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  custom_error_response {
    error_code            = 403
    response_code         = 403
    response_page_path    = ""
    error_caching_min_ttl = 300
  }

  # Default Catch-all: Deny (No Cache, No access)
  default_cache_behavior {
    target_origin_id       = "S3-${aws_s3_bucket.ott_content.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    trusted_key_groups     = [aws_cloudfront_key_group.auth_group.id]

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 0
  }

  # Behavior: Main HLS Playlists
  ordered_cache_behavior {
    path_pattern           = "content/*/main/hls/*.m3u8"
    target_origin_id       = "S3-${aws_s3_bucket.ott_content.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    trusted_key_groups     = [aws_cloudfront_key_group.auth_group.id]

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 0
    default_ttl = 30
    max_ttl     = 60
  }

  # Behavior: Main HLS TS Segments
  ordered_cache_behavior {
    path_pattern           = "content/*/main/hls/*.ts"
    target_origin_id       = "S3-${aws_s3_bucket.ott_content.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    trusted_key_groups     = [aws_cloudfront_key_group.auth_group.id]

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 86400
    default_ttl = 86400
    max_ttl     = 31536000
  }

  # Behavior: Main HLS fMP4 Segments
  ordered_cache_behavior {
    path_pattern           = "content/*/main/hls/*.m4s"
    target_origin_id       = "S3-${aws_s3_bucket.ott_content.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    trusted_key_groups     = [aws_cloudfront_key_group.auth_group.id]

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 86400
    default_ttl = 86400
    max_ttl     = 31536000
  }

  # Behavior: Trailers
  ordered_cache_behavior {
    path_pattern           = "content/*/trailer/hls/*"
    target_origin_id       = "S3-${aws_s3_bucket.ott_content.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 0
    default_ttl = 300
    max_ttl     = 3600
  }
}

# -----------------------------------------------------------------------------
# MediaConvert SNS & IAM
# -----------------------------------------------------------------------------
resource "aws_sns_topic" "mediaconvert_events" {
  name = "${var.environment}-mediaconvert-events"
}

resource "aws_cloudwatch_event_rule" "mediaconvert_rule" {
  name        = "${var.environment}-mediaconvert-events"
  description = "Capture MediaConvert state changes"
  event_pattern = jsonencode({
    source      = ["aws.mediaconvert"]
    detail-type = ["MediaConvert Job State Change"]
  })
}

resource "aws_cloudwatch_event_target" "sns_target" {
  rule      = aws_cloudwatch_event_rule.mediaconvert_rule.name
  target_id = "SendToSNS"
  arn       = aws_sns_topic.mediaconvert_events.arn
}

resource "aws_sns_topic_policy" "default" {
  arn = aws_sns_topic.mediaconvert_events.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "TrustEventBridge"
        Effect    = "Allow"
        Principal = { Service = "events.amazonaws.com" }
        Action    = "sns:Publish"
        Resource  = aws_sns_topic.mediaconvert_events.arn
      }
    ]
  })
}

# IAM Role for MediaConvert
resource "aws_iam_role" "mediaconvert_role" {
  name = "${var.environment}-mediaconvert-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "mediaconvert.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "mediaconvert_policy" {
  name = "${var.environment}-mediaconvert-policy"
  role = aws_iam_role.mediaconvert_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.ott_content.arn}/uploads/*"
      },
      {
        Effect   = "Allow"
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.ott_content.arn}/content/*"
      }
    ]
  })
}
