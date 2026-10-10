package com.ott.platform.ui.screens.search

import com.ott.platform.data.model.Content
import com.ott.platform.data.repository.ContentRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private lateinit var contentRepository: ContentRepository
    private lateinit var viewModel: SearchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        contentRepository = mockk()
        viewModel = SearchViewModel(contentRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty query resets state`() = runTest {
        viewModel.onQueryChange("")
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertFalse(state.isSearching)
        assertTrue(state.results.isEmpty())
        assertFalse(state.hasSearched)
    }

    @Test
    fun `debounce avoids multiple searches`() = runTest {
        val results = listOf(Content(id = "1", title = "Movie"))
        coEvery { contentRepository.searchContent(any(), any()) } returns results

        viewModel.onQueryChange("M")
        viewModel.onQueryChange("Mo")
        viewModel.onQueryChange("Movie")
        
        advanceTimeBy(400) // Before debounce triggers
        var state = viewModel.uiState.value
        assertFalse(state.isSearching)
        
        advanceTimeBy(200) // After debounce triggers (500ms total)
        state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertEquals(results, state.results)
        assertTrue(state.hasSearched)
    }
}
