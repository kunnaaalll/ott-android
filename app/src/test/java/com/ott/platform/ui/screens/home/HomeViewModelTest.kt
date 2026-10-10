package com.ott.platform.ui.screens.home

import com.ott.platform.data.model.Content
import com.ott.platform.data.repository.ContentRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var contentRepository: ContentRepository
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        contentRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadHomeContent populates rows successfully`() = runTest {
        val mockContent = listOf(Content(id = "1"))
        coEvery { contentRepository.getFeaturedContent(any()) } returns mockContent
        coEvery { contentRepository.getTrendingContent(any(), any()) } returns mockContent
        coEvery { contentRepository.getPopularContent(any(), any()) } returns mockContent
        coEvery { contentRepository.getNewReleases(any(), any()) } returns mockContent

        viewModel = HomeViewModel(contentRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mockContent, state.featured)
        assertEquals(mockContent, state.trending)
        assertEquals(mockContent, state.popular)
        assertEquals(mockContent, state.newReleases)
        assertNull(state.error)
    }
}
