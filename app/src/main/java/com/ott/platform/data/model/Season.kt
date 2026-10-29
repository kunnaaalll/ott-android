package com.ott.platform.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Episode(
    @DocumentId
    val id: String = "",
    val seriesId: String = "",
    val seasonId: String = "",
    val episodeNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    val duration: Long = 0L,
    val tenantId: String = "",
    val status: String = ContentStatus.PUBLISHED.name,
    val mainPlaybackAsset: VideoAsset? = null,
    val posterUrl: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun formattedDuration(): String {
        if (duration <= 0) return "21m"
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

data class Season(
    @DocumentId
    val id: String = "",
    val seriesId: String = "",
    val seasonNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    val tenantId: String = "",
    val episodeCount: Int = 0,
    val status: String = ContentStatus.PUBLISHED.name,
    val episodes: List<Episode> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class SeriesHierarchy(
    val series: Content,
    val seasons: List<Season>
)

