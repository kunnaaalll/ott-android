package com.ott.platform.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore: content/{contentId}
 *
 * A single content item (movie, episode, etc.) in the catalogue.
 *
 * Note: streamUrl should NOT contain a permanent public URL in production.
 * It references the CloudFront/S3 path; actual playback URLs are signed at request time.
 */
data class Content(
    @DocumentId
    val id: String = "",

    @PropertyName("tenantId")
    val tenantId: String = "",

    val title: String = "",
    val description: String = "",

    @PropertyName("posterUrl")
    val posterUrl: String = "",

    @PropertyName("backdropUrl")
    val backdropUrl: String = "",

    @PropertyName("trailerPlaybackAsset")
    val trailerPlaybackAsset: VideoAsset? = null,

    @PropertyName("mainPlaybackAsset")
    val mainPlaybackAsset: VideoAsset? = null,

    val duration: Long = 0L, // Duration in seconds

    @PropertyName("releaseDate")
    val releaseDate: Timestamp? = null,

    @PropertyName("categoryIds")
    val categoryIds: List<String> = emptyList(),

    val language: String = "",

    val featured: Boolean = false,

    @PropertyName("isTrending")
    val isTrending: Boolean = false,

    @PropertyName("isNewRelease")
    val isNewRelease: Boolean = false,

    val status: String = ContentStatus.DRAFT.name,

    @ServerTimestamp
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @PropertyName("updatedAt")
    val updatedAt: Timestamp? = null,
) {
    /**
     * Returns formatted duration like "1h 42m"
     */
    fun formattedDuration(): String {
        if (duration <= 0) return ""
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    fun isPublished(): Boolean = ContentStatus.fromString(status) == ContentStatus.PUBLISHED
}
