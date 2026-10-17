package com.ott.platform.data.repository

import com.ott.platform.data.model.PlaybackAuthorization

interface PlaybackAuthorizationRepository {
    suspend fun authorizePlayback(
        contentId: String,
        isTrailer: Boolean = false,
        seriesId: String? = null,
        seasonId: String? = null
    ): Result<PlaybackAuthorization>

    suspend fun authorizePlayback(
        contentId: String,
        isTrailer: Boolean
    ): Result<PlaybackAuthorization> = authorizePlayback(contentId, isTrailer, null, null)
}
