package com.ott.platform.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.BuildConfig
import com.ott.platform.data.model.Content
import com.ott.platform.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val featured: List<Content> = emptyList(),
    val continueWatching: List<Content> = emptyList(), // Optional for now
    val trending: List<Content> = emptyList(),
    val popular: List<Content> = emptyList(),
    val newReleases: List<Content> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val tenantId = BuildConfig.TENANT_ID

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val featured = contentRepository.getFeaturedContent(tenantId)
                val trending = contentRepository.getTrendingContent(tenantId)
                val popular = contentRepository.getPopularContent(tenantId)
                val newReleases = contentRepository.getNewReleases(tenantId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featured = featured,
                    trending = trending,
                    popular = popular,
                    newReleases = newReleases
                )
            } catch (e: Exception) {
                // If one fails, try to recover or just fail all for MVP
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load home content"
                )
            }
        }
    }
}
