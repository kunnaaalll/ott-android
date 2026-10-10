package com.ott.platform.data.model

/**
 * Represents a short-lived playback authorization returned by the secure backend.
 */
data class PlaybackAuthorization(
    val authorized: Boolean = false,
    val playbackUrl: String = "",
    val expiresAt: Long = 0L,
    val cookies: Map<String, String> = emptyMap(),
    val reason: String? = null // For error handling if not authorized (e.g. "SUBSCRIPTION_REQUIRED")
)
