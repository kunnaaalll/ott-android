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
    val error: String? = null
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
                val content = contentRepository.observeContentById(contentId).firstOrNull()
                if (content != null) {
                    // Fetch related content
                    val related = if (content.categoryIds.isNotEmpty()) {
                        contentRepository.getContentByCategory(tenantId, content.categoryIds.first())
                            .filter { it.id != contentId }
                    } else emptyList()

                    _uiState.update { 
                        it.copy(
                            content = content,
                            relatedContent = related,
                            isLoading = false
                        )
                    }
                    
                    // Observe MyList state for this content
                    val currentUserId = userId
                    if (currentUserId != null) {
                        launch {
                            myListRepository.observeIsInMyList(currentUserId, contentId)
                                .collectLatest { inList ->
                                    _uiState.update { it.copy(isInMyList = inList) }
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
