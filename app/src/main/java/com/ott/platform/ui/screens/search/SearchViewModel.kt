package com.ott.platform.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.BuildConfig
import com.ott.platform.data.model.Content
import com.ott.platform.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Content> = emptyList(),
    val error: String? = null,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private val tenantId = BuildConfig.TENANT_ID

    init {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { query ->
                    performSearch(query)
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchQueryFlow.value = newQuery
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update { 
                it.copy(
                    isSearching = false, 
                    results = emptyList(),
                    hasSearched = false,
                    error = null
                )
            }
            return
        }

        _uiState.update { it.copy(isSearching = true, hasSearched = true, error = null) }

        try {
            val results = contentRepository.searchContent(tenantId, query)
            _uiState.update { 
                it.copy(isSearching = false, results = results) 
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isSearching = false, 
                    error = e.localizedMessage ?: "Failed to search content"
                )
            }
        }
    }
}
