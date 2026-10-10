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

    fun authorizeAndPlay(contentId: String) {
        // Prevent race conditions and unnecessary re-authorizations
        if (contentId == currentContentId && _playbackState.value !is PlaybackState.Error) {
            return
        }

        currentContentId = contentId
        _playbackState.value = PlaybackState.Authorizing
        _playbackUrl.value = null
        _cookies.value = emptyMap()

        viewModelScope.launch {
            val result = playbackAuthorizationRepository.authorizePlayback(contentId)
            
            // Check if the content ID has changed while we were waiting
            if (currentContentId != contentId) {
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
        currentContentId?.let {
            currentContentId = null // clear to force re-auth
            authorizeAndPlay(it)
        }
    }

    fun clear() {
        currentContentId = null
        _playbackUrl.value = null
        _cookies.value = emptyMap()
        _playbackState.value = PlaybackState.Idle
    }
}
