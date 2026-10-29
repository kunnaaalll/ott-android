package com.ott.platform.ui.screens.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.BuildConfig
import com.ott.platform.data.model.Content
import com.ott.platform.data.repository.ContentRepository
import com.ott.platform.data.repository.MyListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContentDetailUiState(
    val content: Content? = null,
    val relatedContent: List<Content> = emptyList(),
    val isInMyList: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val seasons: List<com.ott.platform.data.model.Season> = emptyList(),
    val selectedSeason: com.ott.platform.data.model.Season? = null,
    val isSeries: Boolean = false
)

@HiltViewModel
class ContentDetailViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val myListRepository: MyListRepository,
    private val authRepository: com.ott.platform.data.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentDetailUiState())
    val uiState: StateFlow<ContentDetailUiState> = _uiState.asStateFlow()

    private val userId: String?
        get() = authRepository.getCurrentUserId()
    
    private val tenantId = BuildConfig.TENANT_ID

    fun loadContent(contentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val content = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    contentRepository.observeContentById(contentId).firstOrNull()
                } ?: run {
                    // Fallback to direct catalog lookup if stream timed out
                    contentRepository.getContentByCategory(tenantId, "").firstOrNull { it.id == contentId }
                        ?: contentRepository.getFeaturedContent(tenantId).firstOrNull { it.id == contentId }
                }

                if (content != null) {
                    val isSeries = content.isSeries()
                    var seasonsList = emptyList<com.ott.platform.data.model.Season>()

                    if (isSeries) {
                        try {
                            val hierarchy = contentRepository.getSeriesHierarchy(content.id)
                            if (hierarchy != null && hierarchy.seasons.isNotEmpty()) {
                                seasonsList = hierarchy.seasons
                            }
                        } catch (e: Exception) {
                            // Non-fatal fallback
                        }

                        // Fallback: If server returned no seasons, synthesize Season 1 with Episode 1
                        if (seasonsList.isEmpty()) {
                            val defaultEp = com.ott.platform.data.model.Episode(
                                id = content.id,
                                seriesId = content.id,
                                seasonId = "season_01",
                                episodeNumber = 1,
                                title = "Episode 1: Pilot",
                                description = content.description,
                                duration = content.duration,
                                tenantId = content.tenantId,
                                status = content.status,
                                mainPlaybackAsset = content.mainPlaybackAsset,
                                posterUrl = content.posterUrl.ifBlank { content.backdropUrl }
                            )
                            seasonsList = listOf(
                                com.ott.platform.data.model.Season(
                                    id = "season_01",
                                    seriesId = content.id,
                                    seasonNumber = 1,
                                    title = "Season 1",
                                    description = "Season 1 of ${content.getCleanTitle()}",
                                    tenantId = content.tenantId,
                                    episodeCount = 1,
                                    episodes = listOf(defaultEp)
                                )
                            )
                        }
                    }

                    // Fetch related content safely and deduplicate
                    val related = if (content.categoryIds.isNotEmpty()) {
                        try {
                            contentRepository.getContentByCategory(tenantId, content.categoryIds.first())
                                .filter { it.id != contentId }
                                .distinctBy { it.id }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else emptyList()

                    _uiState.update { 
                        it.copy(
                            content = content,
                            relatedContent = related,
                            isLoading = false,
                            isSeries = isSeries,
                            seasons = seasonsList,
                            selectedSeason = seasonsList.firstOrNull()
                        )
                    }
                    
                    // Observe MyList state for this content safely
                    val currentUserId = userId
                    if (currentUserId != null) {
                        launch {
                            try {
                                myListRepository.observeIsInMyList(currentUserId, contentId)
                                    .catch { emit(false) }
                                    .collectLatest { inList ->
                                        _uiState.update { it.copy(isInMyList = inList) }
                                    }
                            } catch (e: Exception) {
                                // Non-fatal if offline or Firestore denied
                            }
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Content not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun selectSeason(season: com.ott.platform.data.model.Season) {
        _uiState.update { it.copy(selectedSeason = season) }
    }

    fun toggleMyList(contentId: String) {
        val currentUserId = userId ?: return
        val currentInList = _uiState.value.isInMyList
        
        // Optimistic UI update
        _uiState.update { it.copy(isInMyList = !currentInList) }
        
        viewModelScope.launch {
            try {
                if (currentInList) {
                    myListRepository.removeFromMyList(currentUserId, contentId)
                } else {
                    myListRepository.addToMyList(currentUserId, contentId)
                }
            } catch (e: Exception) {
                // Revert on failure
                _uiState.update { it.copy(isInMyList = currentInList) }
            }
        }
    }
}
