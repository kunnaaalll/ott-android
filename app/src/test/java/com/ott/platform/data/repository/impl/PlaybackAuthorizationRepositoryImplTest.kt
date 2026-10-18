package com.ott.platform.data.repository.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.core.session.SessionManager
import com.ott.platform.data.model.PlaybackAuthorization
import io.mockk.coEvery
import io.mockk.coVerify
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
    private lateinit var websiteApiClient: WebsiteApiClient
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var repository: PlaybackAuthorizationRepositoryImpl

    @Before
    fun setup() {
        functions = mockk()
        callableReference = mockk()
        websiteApiClient = mockk()
        sessionManager = mockk()
        auth = mockk()
        mockUser = mockk { every { uid } returns "test-uid" }

        every { functions.getHttpsCallable("authorizePlayback") } returns callableReference
        every { auth.currentUser } returns mockUser
        every { sessionManager.getUserId() } returns "test-uid"
        // By default, let websiteApiClient fail with generic error so tests fall through to functions
        coEvery { websiteApiClient.authorizePlayback(any(), any(), any(), any(), any()) } returns Result.failure(Exception("FALLBACK"))

        repository = PlaybackAuthorizationRepositoryImpl(functions, websiteApiClient, sessionManager, auth)
    }

    @Test
    fun `authorizePlayback when unauthenticated immediately returns UNAUTHENTICATED`() = runBlocking {
        every { auth.currentUser } returns null
        every { sessionManager.getUserId() } returns null

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("UNAUTHENTICATED", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { websiteApiClient.authorizePlayback(any(), any(), any(), any(), any()) }
        io.mockk.verify(exactly = 0) { functions.getHttpsCallable(any()) }
    }

    @Test
    fun `authorizePlayback website API success returns authorization without functions fallback`() = runBlocking {
        val testAuth = PlaybackAuthorization(
            authorized = true,
            playbackUrl = "https://s3.in-west2.purestore.io/chumbak/content-123/master.m3u8",
            expiresAt = 9999999L
        )
        coEvery {
            websiteApiClient.authorizePlayback(
                userId = "test-uid",
                contentId = "content-123",
                isTrailer = false,
                seriesId = null,
                seasonId = null
            )
        } returns Result.success(testAuth)

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isSuccess)
        assertEquals(testAuth.playbackUrl, result.getOrNull()?.playbackUrl)
        io.mockk.verify(exactly = 0) { functions.getHttpsCallable(any()) }
    }

    @Test
    fun `authorizePlayback website API subscription required returns immediately without functions fallback`() = runBlocking {
        coEvery {
            websiteApiClient.authorizePlayback(
                userId = "test-uid",
                contentId = "content-123",
                isTrailer = false,
                seriesId = null,
                seasonId = null
            )
        } returns Result.failure(Exception("SUBSCRIPTION_REQUIRED"))

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("SUBSCRIPTION_REQUIRED", result.exceptionOrNull()?.message)
        io.mockk.verify(exactly = 0) { functions.getHttpsCallable(any()) }
    }

    @Test
    fun `authorizePlayback functions fallback success`() = runBlocking {
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
    fun `authorizePlayback generic exception returns UNKNOWN_ERROR`() = runBlocking {
        val genericException = Exception("Some network issue")
        every { callableReference.call(any()) } throws genericException

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("UNKNOWN_ERROR", result.exceptionOrNull()?.message)
    }

    @Test
    fun `authorizePlayback UNAUTHENTICATED error`() = runBlocking {
        val exception = com.google.firebase.functions.TestFirebaseException("Not logged in", FirebaseFunctionsException.Code.UNAUTHENTICATED)
        every { callableReference.call(any()) } throws exception

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("UNAUTHENTICATED", result.exceptionOrNull()?.message)
    }

    @Test
    fun `authorizePlayback PERMISSION_DENIED error`() = runBlocking {
        val exception = com.google.firebase.functions.TestFirebaseException("Not subscribed", FirebaseFunctionsException.Code.PERMISSION_DENIED)
        every { callableReference.call(any()) } throws exception

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("SUBSCRIPTION_REQUIRED", result.exceptionOrNull()?.message)
    }

    @Test
    fun `authorizePlayback NOT_FOUND error`() = runBlocking {
        val exception = com.google.firebase.functions.TestFirebaseException("Content not found", FirebaseFunctionsException.Code.NOT_FOUND)
        every { callableReference.call(any()) } throws exception

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("CONTENT_UNAVAILABLE", result.exceptionOrNull()?.message)
    }

    @Test
    fun `authorizePlayback other firebase error`() = runBlocking {
        val exception = com.google.firebase.functions.TestFirebaseException("Internal error", FirebaseFunctionsException.Code.INTERNAL)
        every { callableReference.call(any()) } throws exception

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("SERVER_ERROR", result.exceptionOrNull()?.message)
    }

    @Test
    fun `authorizePlayback invalid response format`() = runBlocking {
        val mockData = "not a map"
        val mockResult: HttpsCallableResult = mockk()
        every { mockResult.data } returns mockData
        every { callableReference.call(any()) } returns Tasks.forResult(mockResult)

        val result = repository.authorizePlayback("content-123")

        assertTrue(result.isFailure)
        assertEquals("Invalid response format", result.exceptionOrNull()?.message)
    }
}
