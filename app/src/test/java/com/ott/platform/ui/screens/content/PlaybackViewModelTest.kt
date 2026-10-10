package com.ott.platform.ui.screens.content

import app.cash.turbine.test
import com.ott.platform.data.model.PlaybackAuthorization
import com.ott.platform.data.repository.PlaybackAuthorizationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModelTest {

    private lateinit var repository: PlaybackAuthorizationRepository
    private lateinit var viewModel: PlaybackViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = PlaybackViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Happy Path ────────────────────────────────────────────────────────────

    @Test
    fun `authorizeAndPlay success updates state to Preparing with url and cookies`() = runTest {
        val testCookies = mapOf(
            "CloudFront-Policy" to "policy123",
            "CloudFront-Signature" to "sig123",
            "CloudFront-Key-Pair-Id" to "key123"
        )
        coEvery { repository.authorizePlayback("content-123", any()) } returns Result.success(
            PlaybackAuthorization(
                authorized = true,
                playbackUrl = "https://d12345.cloudfront.net/content/t1/content-123/main/hls/master.m3u8",
                expiresAt = System.currentTimeMillis() + 900_000L, // 15 minutes in future
                cookies = testCookies
            )
        )

        viewModel.playbackState.test {
            assertEquals(PlaybackState.Idle, awaitItem())
            viewModel.authorizeAndPlay("content-123")
            assertEquals(PlaybackState.Authorizing, awaitItem())
            assertEquals(PlaybackState.Preparing, awaitItem())
        }

        // Verify URL and cookies are set
        assertEquals(
            "https://d12345.cloudfront.net/content/t1/content-123/main/hls/master.m3u8",
            viewModel.playbackUrl.value
        )
        assertEquals(testCookies, viewModel.cookies.value)
    }

    // ── Error Handling ────────────────────────────────────────────────────────

    @Test
    fun `authorizeAndPlay subscription required updates state to Error`() = runTest {
        coEvery { repository.authorizePlayback("content-123", any()) } returns Result.failure(
            Exception("SUBSCRIPTION_REQUIRED")
        )

        viewModel.playbackState.test {
            assertEquals(PlaybackState.Idle, awaitItem())
            viewModel.authorizeAndPlay("content-123")
            assertEquals(PlaybackState.Authorizing, awaitItem())
            val errorState = awaitItem() as PlaybackState.Error
            assertEquals("Your subscription is required to watch this content.", errorState.message)
            assertEquals("SUBSCRIPTION_REQUIRED", errorState.reason)
        }
    }

    @Test
    fun `authorizeAndPlay unauthenticated updates state to Error with correct message`() = runTest {
        coEvery { repository.authorizePlayback("content-123", any()) } returns Result.failure(
            Exception("UNAUTHENTICATED")
        )

        viewModel.playbackState.test {
            assertEquals(PlaybackState.Idle, awaitItem())
            viewModel.authorizeAndPlay("content-123")
            assertEquals(PlaybackState.Authorizing, awaitItem())
            val errorState = awaitItem() as PlaybackState.Error
            assertEquals("Please sign in to continue.", errorState.message)
        }
    }

    @Test
    fun `authorizeAndPlay content unavailable shows generic message`() = runTest {
        coEvery { repository.authorizePlayback("content-123", any()) } returns Result.failure(
            Exception("CONTENT_UNAVAILABLE")
        )

        viewModel.playbackState.test {
            assertEquals(PlaybackState.Idle, awaitItem())
            viewModel.authorizeAndPlay("content-123")
            assertEquals(PlaybackState.Authorizing, awaitItem())
            val errorState = awaitItem() as PlaybackState.Error
            assertEquals("This content is currently unavailable.", errorState.message)
        }
    }

    // ── State Management / Lifecycle ──────────────────────────────────────────

    @Test
    fun `second call to authorizeAndPlay for same contentId is a no-op when not in error`() = runTest {
        var callCount = 0
        coEvery { repository.authorizePlayback("content-123", any()) } coAnswers {
            callCount++
            Result.success(PlaybackAuthorization(true, "url", 0L))
        }

        viewModel.playbackState.test {
            assertEquals(PlaybackState.Idle, awaitItem())
            viewModel.authorizeAndPlay("content-123")
            assertEquals(PlaybackState.Authorizing, awaitItem())
            assertEquals(PlaybackState.Preparing, awaitItem())

            // Second call with same ID — should be a no-op
            viewModel.authorizeAndPlay("content-123")
            expectNoEvents()
        }

        // Repository should only have been called once
        assertEquals(1, callCount)
    }

    @Test
    fun `clear resets all state and credentials`() = runTest {
        coEvery { repository.authorizePlayback("content-123", any()) } returns Result.success(
            PlaybackAuthorization(
                authorized = true,
                playbackUrl = "https://d12345.cloudfront.net/content/t1/content-123/main/hls/master.m3u8",
                expiresAt = System.currentTimeMillis() + 900_000L,
                cookies = mapOf("CloudFront-Policy" to "policy123")
            )
        )

        viewModel.playbackState.test {
            assertEquals(PlaybackState.Idle, awaitItem())
            viewModel.authorizeAndPlay("content-123")
            awaitItem() // Authorizing
            awaitItem() // Preparing

            // Clear — simulates logout or navigation away
            viewModel.clear()
            assertEquals(PlaybackState.Idle, awaitItem())
        }

        // Verify credentials are cleared — no stale cookies remain
        assertNull(viewModel.playbackUrl.value)
        assertTrue(viewModel.cookies.value.isEmpty())
    }

    @Test
    fun `retry re-authorizes after error`() = runTest {
        var callCount = 0
        coEvery { repository.authorizePlayback("content-123", any()) } coAnswers {
            callCount++
            if (callCount == 1) {
                Result.failure(Exception("SERVER_ERROR"))
            } else {
                Result.success(PlaybackAuthorization(true, "url", 0L))
            }
        }

        viewModel.playbackState.test {
            assertEquals(PlaybackState.Idle, awaitItem())
            viewModel.authorizeAndPlay("content-123")
            assertEquals(PlaybackState.Authorizing, awaitItem())
            awaitItem() as PlaybackState.Error // Error state

            viewModel.retry()
            assertEquals(PlaybackState.Authorizing, awaitItem())
            assertEquals(PlaybackState.Preparing, awaitItem())
        }

        assertEquals(2, callCount)
    }

    @Test
    fun `stale authorization response for previous content is discarded`() = runTest {
        // This simulates a race condition: user navigates to content-123, then quickly
        // navigates to content-456 before the first response arrives.
        coEvery { repository.authorizePlayback("content-123", any()) } returns Result.success(
            PlaybackAuthorization(true, "url-for-123", 0L)
        )
        coEvery { repository.authorizePlayback("content-456", any()) } returns Result.success(
            PlaybackAuthorization(true, "url-for-456", 0L)
        )

        viewModel.playbackState.test {
            assertEquals(PlaybackState.Idle, awaitItem())
            viewModel.authorizeAndPlay("content-123")
            // Immediately switch to a different content before first response
            viewModel.authorizeAndPlay("content-456")
            
            awaitItem() // Authorizing
            
            val finalState = awaitItem()
            assertEquals(PlaybackState.Preparing, finalState)
        }

        // Final URL must be for content-456, not the stale content-123 response
        assertEquals("url-for-456", viewModel.playbackUrl.value)
    }

    // ── Cookie Security ───────────────────────────────────────────────────────

    @Test
    fun `cookies must only contain CloudFront cookie keys`() = runTest {
        val testCookies = mapOf(
            "CloudFront-Policy" to "policy123",
            "CloudFront-Signature" to "sig123",
            "CloudFront-Key-Pair-Id" to "key123"
        )
        coEvery { repository.authorizePlayback("content-123", any()) } returns Result.success(
            PlaybackAuthorization(true, "https://d.cloudfront.net/content/t1/c1/main/hls/master.m3u8", 0L, testCookies)
        )

        viewModel.authorizeAndPlay("content-123")
        testDispatcher.scheduler.advanceUntilIdle()

        val cookies = viewModel.cookies.value
        // Verify only the expected CloudFront cookie keys are present
        assertTrue(cookies.containsKey("CloudFront-Policy"))
        assertTrue(cookies.containsKey("CloudFront-Signature"))
        assertTrue(cookies.containsKey("CloudFront-Key-Pair-Id"))
        // No extra credentials should be included
        assertEquals(3, cookies.size)
        // The private key itself must not be present
        cookies.values.forEach { value ->
            assertFalse("Cookie value should not look like a private key", value.startsWith("-----BEGIN"))
        }
    }
}
