package com.ott.platform.ui.screens.auth

import app.cash.turbine.test
import com.ott.platform.core.analytics.AnalyticsTracker
import com.ott.platform.core.crash.CrashReporter
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private lateinit var classUnderTest: AuthViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val currentUserFlow = MutableStateFlow<FirebaseUser?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.currentUser } returns currentUserFlow
        every { authRepository.getCurrentUserId() } returns null
        classUnderTest = AuthViewModel(authRepository, userRepository, crashReporter, analyticsTracker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle or Unauthenticated depending on user`() = runTest {
        classUnderTest.authState.test {
            val state = awaitItem()
            assertTrue(state is AuthState.Idle || state is AuthState.Unauthenticated)
        }
    }

    @Test
    fun `signIn success updates state and tracks analytics`() = runTest {
        val mockUser = mockk<FirebaseUser> {
            coEvery { uid } returns "test_uid"
        }
        coEvery { authRepository.signInWithEmail("test@test.com", "password") } returns Result.success(mockUser)

        classUnderTest.signIn("test@test.com", "password")

        classUnderTest.authState.test {
            // Wait for initial
            awaitItem()
            // Then loading
            assertTrue(awaitItem() is AuthState.Loading)
            // Auth flow might not update authState directly on success since it relies on currentUser listener,
            // but the test can verify interactions
            cancelAndIgnoreRemainingEvents()
        }

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { analyticsTracker.logEvent("login_started") }
        coVerify { analyticsTracker.logEvent("login_success") }
        coVerify { userRepository.updateLastLogin("test_uid") }
    }

    @Test
    fun `signIn failure updates state with Error`() = runTest {
        val error = Exception("Invalid credentials")
        coEvery { authRepository.signInWithEmail("test@test.com", "password") } returns Result.failure(error)

        classUnderTest.signIn("test@test.com", "password")

        classUnderTest.authState.test {
            awaitItem() // initial
            assertTrue(awaitItem() is AuthState.Loading)
            val errorState = awaitItem() as AuthState.Error
            assertEquals("Invalid credentials", errorState.message)
        }

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { crashReporter.logException(error) }
        coVerify { analyticsTracker.logEvent("login_failed") }
    }

    @Test
    fun `register success creates user profile`() = runTest {
        val mockUser = mockk<FirebaseUser> {
            coEvery { uid } returns "test_uid"
            coEvery { email } returns "test@test.com"
            coEvery { displayName } returns "Test User"
            coEvery { photoUrl } returns null
        }
        coEvery { authRepository.registerWithEmail("test@test.com", "password") } returns Result.success(mockUser)

        classUnderTest.register("test@test.com", "password")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userRepository.createOrUpdateUser(any()) }
        coVerify { analyticsTracker.logEvent("registration_success") }
    }

    @Test
    fun `signInWithGoogleAccount success updates authState to Authenticated`() = runTest {
        val googleUser = com.ott.platform.data.model.User(
            id = "usr_google_123",
            email = "user@gmail.com",
            name = "Google User",
            tenantId = "tenant_chumbak"
        )
        coEvery {
            authRepository.signInWithGoogleAccount(
                email = "user@gmail.com",
                displayName = "Google User",
                photoUrl = "https://example.com/photo.jpg",
                googleId = "g_123"
            )
        } returns Result.success(googleUser)

        classUnderTest.signInWithGoogleAccount(
            email = "user@gmail.com",
            displayName = "Google User",
            photoUrl = "https://example.com/photo.jpg",
            googleId = "g_123"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(classUnderTest.authState.value is AuthState.Authenticated)
        assertEquals("usr_google_123", (classUnderTest.authState.value as AuthState.Authenticated).uid)
        coVerify { analyticsTracker.logEvent("google_login_success") }
    }

    @Test
    fun `signInWithGoogleAccount failure updates authState to Error`() = runTest {
        coEvery {
            authRepository.signInWithGoogleAccount(any(), any(), any(), any())
        } returns Result.failure(Exception("Google server unreachable"))

        classUnderTest.signInWithGoogleAccount("err@gmail.com", "Error User")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(classUnderTest.authState.value is AuthState.Error)
        assertEquals("Google server unreachable", (classUnderTest.authState.value as AuthState.Error).message)
        coVerify { analyticsTracker.logEvent("google_login_failed") }
    }
}
