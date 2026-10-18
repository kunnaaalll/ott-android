package com.ott.platform.ui.screens.home

import app.cash.turbine.test
import com.ott.platform.data.model.PlaybackAuthorization
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.PlaybackAuthorizationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeroTrailerControllerTest {

    private lateinit var repository: PlaybackAuthorizationRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var controller: HeroTrailerController
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        authRepository = mockk()
        every { authRepository.getCurrentUserId() } returns "user-123"
        controller = HeroTrailerController(repository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onHeroActive triggers authorization after delay when authenticated`() = runTest {
        coEvery { repository.authorizePlayback("hero-123", true) } returns Result.success(
            PlaybackAuthorization(true, "url", 0L)
        )

        controller.playbackUrl.test {
            assertEquals(null, awaitItem())
            
            controller.onHeroActive("hero-123")
            
            // Should not authorize immediately due to debounce
            advanceTimeBy(500)
            expectNoEvents()
            
            // After 1000ms delay, it should authorize
            advanceTimeBy(600)
            assertEquals("url", awaitItem())
        }
    }

    @Test
    fun `onHeroActive does not authorize when unauthenticated`() = runTest {
        every { authRepository.getCurrentUserId() } returns null

        controller.playbackUrl.test {
            assertEquals(null, awaitItem())
            
            controller.onHeroActive("hero-123")
            advanceTimeBy(1500)
            
            // No authorization should occur
            expectNoEvents()
        }

        coVerify(exactly = 0) { repository.authorizePlayback(any(), any(), any(), any()) }
    }

    @Test
    fun `onHeroInactive cancels authorization`() = runTest {
        coEvery { repository.authorizePlayback("hero-123", true) } returns Result.success(
            PlaybackAuthorization(true, "url", 0L)
        )

        controller.playbackUrl.test {
            assertEquals(null, awaitItem())
            
            controller.onHeroActive("hero-123")
            advanceTimeBy(500)
            
            controller.onHeroInactive("hero-123")
            advanceTimeBy(600)
            
            // We shouldn't get the 'url' because it was cancelled
            expectNoEvents()
        }
    }
}
