package com.ott.platform.data.model

/**
 * Represents a video asset (main content, trailer) stored in the platform's infrastructure.
 * This represents the *logical* asset, not the final authorized playback URL.
 * 
 * E.g. hlsPath = "content/tenant_001/movie_123/hls/master.m3u8"
 */
data class VideoAsset(
    val contentId: String = "",
    val tenantId: String = "",
    val assetType: String = "", // e.g., "MAIN", "TRAILER"
    val hlsPath: String = "",
    val duration: Long = 0L // Duration in seconds
)
