package com.ott.platform.ui.screens.mylist

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
class MyListViewModelTest {

    private lateinit var myListRepository: MyListRepository
    private lateinit var contentRepository: ContentRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: MyListViewModel
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
    fun `loadMyList populates items when user is logged in`() = runTest {
        val mockContent = Content(id = "1", title = "Movie")
        every { authRepository.getCurrentUserId() } returns "user1"
        every { myListRepository.observeMyListIds("user1") } returns flowOf(listOf("1"))
        every { contentRepository.observeContentById("1") } returns flowOf(mockContent)

        viewModel = MyListViewModel(myListRepository, contentRepository, authRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.items.size)
        assertEquals("Movie", state.items.first().title)
        assertNull(state.error)
    }

    @Test
    fun `loadMyList sets error when user not logged in`() = runTest {
        every { authRepository.getCurrentUserId() } returns null

        viewModel = MyListViewModel(myListRepository, contentRepository, authRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("User not logged in", state.error)
    }
}
