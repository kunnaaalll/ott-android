package com.ott.platform.data.repository

import com.ott.platform.data.model.PlaybackAuthorization

interface PlaybackAuthorizationRepository {
    suspend fun authorizePlayback(contentId: String, isTrailer: Boolean = false): Result<PlaybackAuthorization>
}
