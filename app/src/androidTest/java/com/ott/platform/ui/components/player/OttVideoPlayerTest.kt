package com.ott.platform.ui.components.player

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ott.platform.ui.screens.content.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OttVideoPlayerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testValidCloudFrontHostIsAccepted() {
        var state: PlaybackState? = null
        val configuredHost = com.ott.platform.BuildConfig.CLOUDFRONT_HOSTNAME
        
        composeTestRule.setContent {
            OttVideoPlayer(
                playbackState = PlaybackState.Preparing,
                playbackUrl = "https://$configuredHost/video.m3u8",
                cookies = mapOf("CloudFront-Key-Pair-Id" to "123"),
                onStateChanged = { state = it },
                onRetry = {},
                onGoToSubscription = {}
            )
        }
        
        composeTestRule.waitForIdle()
        // If it was accepted, it won't emit an Error state for INVALID_HOST
        // It might be Buffering, Preparing, or an ExoPlayer Error if it can't load the fake URL,
        // but it won't be our specific INVALID_HOST error.
        if (state is PlaybackState.Error) {
            val err = state as PlaybackState.Error
            assert(err.reason != "INVALID_HOST" && err.reason != "INSECURE_CONNECTION")
        }
    }

    @Test
    fun testUnconfiguredCloudFrontHostIsRejected() {
        var state: PlaybackState? = null
        
        composeTestRule.setContent {
            OttVideoPlayer(
                playbackState = PlaybackState.Preparing,
                playbackUrl = "https://unrelated.cloudfront.net/video.m3u8",
                cookies = mapOf("CloudFront-Key-Pair-Id" to "123"),
                onStateChanged = { state = it },
                onRetry = {},
                onGoToSubscription = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        val err = state as PlaybackState.Error
        assertEquals("INVALID_HOST", err.reason)
    }

    @Test
    fun testAttackerHostnameIsRejected() {
        var state: PlaybackState? = null
        
        composeTestRule.setContent {
            OttVideoPlayer(
                playbackState = PlaybackState.Preparing,
                playbackUrl = "https://attacker-cloudfront.com/video.m3u8",
                cookies = mapOf("CloudFront-Key-Pair-Id" to "123"),
                onStateChanged = { state = it },
                onRetry = {},
                onGoToSubscription = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        val err = state as PlaybackState.Error
        assertEquals("INVALID_HOST", err.reason)
    }

    @Test
    fun testHttpIsRejected() {
        var state: PlaybackState? = null
        val configuredHost = com.ott.platform.BuildConfig.CLOUDFRONT_HOSTNAME
        
        composeTestRule.setContent {
            OttVideoPlayer(
                playbackState = PlaybackState.Preparing,
                playbackUrl = "http://$configuredHost/video.m3u8", // Insecure
                cookies = mapOf("CloudFront-Key-Pair-Id" to "123"),
                onStateChanged = { state = it },
                onRetry = {},
                onGoToSubscription = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        val err = state as PlaybackState.Error
        assertEquals("INSECURE_CONNECTION", err.reason)
    }

    @Test
    fun testMalformedMissingHostIsRejected() {
        var state: PlaybackState? = null
        
        composeTestRule.setContent {
            OttVideoPlayer(
                playbackState = PlaybackState.Preparing,
                playbackUrl = "https:///video.m3u8", // Missing host
                cookies = mapOf("CloudFront-Key-Pair-Id" to "123"),
                onStateChanged = { state = it },
                onRetry = {},
                onGoToSubscription = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        val err = state as PlaybackState.Error
        assertEquals("INVALID_HOST", err.reason)
    }
}
