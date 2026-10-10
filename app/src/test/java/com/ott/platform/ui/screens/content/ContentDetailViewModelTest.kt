package com.ott.platform.ui.screens.content

import com.ott.platform.data.model.Content
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.ContentRepository
import com.ott.platform.data.repository.MyListRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContentDetailViewModelTest {

    private lateinit var myListRepository: MyListRepository
    private lateinit var contentRepository: ContentRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: ContentDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        myListRepository = mockk()
        contentRepository = mockk()
        authRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadContent populates content and related content`() = runTest {
        val mockContent = Content(id = "1", title = "Movie", categoryIds = listOf("cat1"))
        val mockRelated = Content(id = "2", title = "Movie 2", categoryIds = listOf("cat1"))
        
        every { authRepository.getCurrentUserId() } returns "user1"
        every { contentRepository.observeContentById("1") } returns flowOf(mockContent)
        coEvery { contentRepository.getContentByCategory(any(), "cat1") } returns listOf(mockContent, mockRelated)
        every { myListRepository.observeIsInMyList("user1", "1") } returns flowOf(true)

        viewModel = ContentDetailViewModel(contentRepository, myListRepository, authRepository)
        viewModel.loadContent("1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mockContent, state.content)
        assertEquals(1, state.relatedContent.size)
        assertEquals("Movie 2", state.relatedContent.first().title)
        assertTrue(state.isInMyList)
        assertNull(state.error)
    }
}
