package com.ott.platform.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.repository.PlaybackAuthorizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HeroTrailerController @Inject constructor(
    private val playbackAuthorizationRepository: PlaybackAuthorizationRepository
) : ViewModel() {

    private val _playbackUrl = MutableStateFlow<String?>(null)
    val playbackUrl: StateFlow<String?> = _playbackUrl.asStateFlow()

    private val _cookies = MutableStateFlow<Map<String, String>>(emptyMap())
    val cookies: StateFlow<Map<String, String>> = _cookies.asStateFlow()

    private val _isMuted = MutableStateFlow(true)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var currentActiveContentId: String? = null
    private var authorizationJob: Job? = null

    fun onHeroActive(contentId: String) {
        if (contentId == currentActiveContentId) return
        
        currentActiveContentId = contentId
        _playbackUrl.value = null
        _cookies.value = emptyMap()
        _isMuted.value = true // Reset mute state for new trailers
        
        authorizationJob?.cancel()
        authorizationJob = viewModelScope.launch {
            // Debounce for 1 second before requesting authorization
            delay(1000)
            
            // Assume we use a suffix or a different logic to get trailer content id, 
            // but for now we pass the contentId to get its authorization. 
            // In a real scenario, this would likely be specific to the trailer.
            val result = playbackAuthorizationRepository.authorizePlayback(contentId, isTrailer = true)
            
            // Check if active content changed while waiting
            if (currentActiveContentId != contentId) return@launch
            
            result.onSuccess { auth ->
                if (auth.authorized && auth.playbackUrl.isNotEmpty()) {
                    _playbackUrl.value = auth.playbackUrl
                    _cookies.value = auth.cookies
                }
            }
        }
    }

    fun onHeroInactive(contentId: String) {
        if (contentId == currentActiveContentId) {
            authorizationJob?.cancel()
            _playbackUrl.value = null
            _cookies.value = emptyMap()
            currentActiveContentId = null
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    override fun onCleared() {
        super.onCleared()
        authorizationJob?.cancel()
    }
}
