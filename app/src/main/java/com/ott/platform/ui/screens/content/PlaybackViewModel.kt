package com.ott.platform.ui.screens.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.repository.PlaybackAuthorizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackAuthorizationRepository: PlaybackAuthorizationRepository
) : ViewModel() {

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playbackUrl = MutableStateFlow<String?>(null)
    val playbackUrl: StateFlow<String?> = _playbackUrl.asStateFlow()

    private val _cookies = MutableStateFlow<Map<String, String>>(emptyMap())
    val cookies: StateFlow<Map<String, String>> = _cookies.asStateFlow()

    private var currentContentId: String? = null
    private var currentIsTrailer: Boolean = false
    private var currentSeriesId: String? = null
    private var currentSeasonId: String? = null

    fun authorizeAndPlay(
        contentId: String,
        isTrailer: Boolean = false,
        seriesId: String? = null,
        seasonId: String? = null
    ) {
        val isSameRequest = contentId == currentContentId &&
                isTrailer == currentIsTrailer &&
                seriesId == currentSeriesId &&
                seasonId == currentSeasonId

        // Prevent race conditions and unnecessary re-authorizations
        if (isSameRequest && _playbackState.value !is PlaybackState.Error) {
            return
        }

        currentContentId = contentId
        currentIsTrailer = isTrailer
        currentSeriesId = seriesId
        currentSeasonId = seasonId

        _playbackState.value = PlaybackState.Authorizing
        _playbackUrl.value = null
        _cookies.value = emptyMap()

        viewModelScope.launch {
            val result = playbackAuthorizationRepository.authorizePlayback(
                contentId = contentId,
                isTrailer = isTrailer,
                seriesId = seriesId,
                seasonId = seasonId
            )
            
            // Check if the parameters have changed while we were waiting
            if (currentContentId != contentId ||
                currentIsTrailer != isTrailer ||
                currentSeriesId != seriesId ||
                currentSeasonId != seasonId
            ) {
                return@launch
            }

            result.onSuccess { auth ->
                if (auth.authorized && auth.playbackUrl.isNotEmpty()) {
                    _playbackUrl.value = auth.playbackUrl
                    _cookies.value = auth.cookies
                    _playbackState.value = PlaybackState.Preparing
                } else {
                    _playbackState.value = PlaybackState.Error(
                        message = "Playback not authorized.",
                        reason = auth.reason
                    )
                }
            }.onFailure {
                val reason = it.message
                val displayMessage = when (reason) {
                    "UNAUTHENTICATED" -> "Please sign in to continue."
                    "SUBSCRIPTION_REQUIRED" -> "Your subscription is required to watch this content."
                    "CONTENT_UNAVAILABLE" -> "This content is currently unavailable."
                    else -> "Playback is temporarily unavailable. Please try again later."
                }
                _playbackState.value = PlaybackState.Error(
                    message = displayMessage,
                    reason = reason
                )
            }
        }
    }

    fun updateState(state: PlaybackState) {
        _playbackState.value = state
    }

    fun retry() {
        val contentId = currentContentId ?: return
        val isTrailer = currentIsTrailer
        val seriesId = currentSeriesId
        val seasonId = currentSeasonId
        currentContentId = null // clear to force re-auth
        authorizeAndPlay(contentId, isTrailer, seriesId, seasonId)
    }

    fun clear() {
        currentContentId = null
        currentIsTrailer = false
        currentSeriesId = null
        currentSeasonId = null
        _playbackUrl.value = null
        _cookies.value = emptyMap()
        _playbackState.value = PlaybackState.Idle
    }
}
