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
    val newReleases: List<Content> = emptyList(),
    val trending: List<Content> = emptyList(),
    val hindiContent: List<Content> = emptyList(),
    val englishContent: List<Content> = emptyList(),
    val comedyContent: List<Content> = emptyList(),
    val actionContent: List<Content> = emptyList(),
    val romanceContent: List<Content> = emptyList(),
    val popular: List<Content> = emptyList(),
    val selectedLanguage: String = "ALL", // "ALL", "Hindi", "English"
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

    fun setLanguage(language: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val featured = contentRepository.getFeaturedContent(tenantId)
                val newReleases = contentRepository.getNewReleases(tenantId, 10)
                val trending = contentRepository.getTrendingContent(tenantId, 10)
                val hindi = contentRepository.getContentByLanguage(tenantId, "Hindi")
                val english = contentRepository.getContentByLanguage(tenantId, "English")
                val comedy = contentRepository.getContentByCategory(tenantId, "cat_comedy")
                val action = contentRepository.getContentByCategory(tenantId, "cat_action")
                val romance = contentRepository.getContentByCategory(tenantId, "cat_romance")
                val popular = contentRepository.getPopularContent(tenantId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featured = featured,
                    newReleases = newReleases,
                    trending = trending,
                    hindiContent = hindi,
                    englishContent = english,
                    comedyContent = comedy,
                    actionContent = action,
                    romanceContent = romance,
                    popular = popular
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
