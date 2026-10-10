package com.ott.platform.ui.screens.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.model.Content
import com.ott.platform.data.repository.ContentRepository
import com.ott.platform.data.repository.MyListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyListUiState(
    val isLoading: Boolean = true,
    val items: List<Content> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val myListRepository: MyListRepository,
    private val contentRepository: ContentRepository,
    private val authRepository: com.ott.platform.data.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListUiState())
    val uiState: StateFlow<MyListUiState> = _uiState.asStateFlow()

    private val userId: String?
        get() = authRepository.getCurrentUserId()

    init {
        loadMyList()
    }

    private fun loadMyList() {
        val currentUserId = userId
        if (currentUserId == null) {
            _uiState.value = MyListUiState(isLoading = false, error = "User not logged in")
            return
        }

        viewModelScope.launch {
            myListRepository.observeMyListIds(currentUserId)
                .map { ids ->
                    ids.mapNotNull { id ->
                        try {
                            contentRepository.observeContentById(id).firstOrNull()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                .catch { e ->
                    _uiState.value = MyListUiState(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to load My List"
                    )
                }
                .collectLatest { contentItems ->
                    _uiState.value = MyListUiState(
                        isLoading = false,
                        items = contentItems
                    )
                }
        }
    }
}
