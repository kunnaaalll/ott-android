package com.ott.platform.data.repository.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackAuthorizationRepositoryImplTest {

    private lateinit var functions: FirebaseFunctions
    private lateinit var callableReference: HttpsCallableReference
    private lateinit var repository: PlaybackAuthorizationRepositoryImpl

    @Before
    fun setup() {
        functions = mockk()
        callableReference = mockk()
        every { functions.getHttpsCallable("authorizePlayback") } returns callableReference
        repository = PlaybackAuthorizationRepositoryImpl(functions)
    }

    @Test
    fun `authorizePlayback success`() = runBlocking {
        val mockData = mapOf(
            "authorized" to true,
            "playbackUrl" to "https://cdn.example.com/hls/master.m3u8",
            "expiresAt" to 1234567890L,
            "cookies" to mapOf(
                "CloudFront-Policy" to "policy123",
                "CloudFront-Signature" to "sig123",
                "CloudFront-Key-Pair-Id" to "key123"
            )
        )
        val mockResult: HttpsCallableResult = mockk()
        every { mockResult.data } returns mockData
        every { callableReference.call(any()) } returns Tasks.forResult(mockResult)

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isSuccess)
        val auth = result.getOrNull()!!
        assertTrue(auth.authorized)
        assertEquals("https://cdn.example.com/hls/master.m3u8", auth.playbackUrl)
        assertEquals("policy123", auth.cookies["CloudFront-Policy"])
        assertEquals("sig123", auth.cookies["CloudFront-Signature"])
        assertEquals("key123", auth.cookies["CloudFront-Key-Pair-Id"])
    }

    @Test
    fun `authorizePlayback subscription required`() = runBlocking {
        val mockException = FirebaseFunctionsException(
            "Permission denied",
            FirebaseFunctionsException.Code.PERMISSION_DENIED
        )
        every { callableReference.call(any()) } returns Tasks.forException(mockException)

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("SUBSCRIPTION_REQUIRED", result.exceptionOrNull()?.message)
    }
}
