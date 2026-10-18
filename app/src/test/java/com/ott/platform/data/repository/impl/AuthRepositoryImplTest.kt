package com.ott.platform.data.repository.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.core.session.SessionManager
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.SubscriptionRepository
import com.ott.platform.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    private val websiteApiClient = mockk<WebsiteApiClient>()
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        coEvery { websiteApiClient.login(any(), any()) } returns Result.failure(Exception("Not found"))
        coEvery { websiteApiClient.register(any(), any(), any(), any()) } returns Result.failure(Exception("Not found"))

        authRepository = AuthRepositoryImpl(
            auth = firebaseAuth,
            websiteApiClient = websiteApiClient,
            sessionManager = sessionManager,
            userRepository = userRepository,
            subscriptionRepository = subscriptionRepository
        )
    }

    @Test
    fun `signInWithEmail returns success when website API authenticates user`() = runTest {
        val mockUser = mockk<FirebaseUser> {
            every { uid } returns "web-uid"
            every { email } returns "test@test.com"
        }
        val mockAuthResult = mockk<AuthResult> {
            every { user } returns mockUser
        }
        every { firebaseAuth.currentUser } returns mockUser
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)

        val websiteUser = User(
            id = "web-uid",
            email = "test@test.com",
            name = "Web User",
            tenantId = "tenant-chumbak"
        )
        val authResponse = com.ott.platform.core.api.WebsiteAuthResult(
            user = websiteUser,
            subscription = null
        )
        coEvery { websiteApiClient.login("test@test.com", "password") } returns Result.success(authResponse)

        val result = authRepository.signInWithEmail("test@test.com", "password")

        assertTrue(result.isSuccess)
        verify { sessionManager.saveSession(websiteUser) }
    }

    @Test
    fun `signInWithEmail returns success on fallback to firebase auth`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "test-uid"
            every { email } returns "test@test.com"
        }
        val mockAuthResult = mockk<AuthResult> {
            every { user } returns mockUser
        }
        
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)

        val result = authRepository.signInWithEmail("test@test.com", "password")

        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
    }

    @Test
    fun `signInWithEmail returns failure when both website and firebase fail`() = runTest {
        val exception = Exception("Network error")
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)

        val result = authRepository.signInWithEmail("test@test.com", "password")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `register returns success when website API registers user`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "new-uid"
            every { email } returns "newuser@test.com"
        }
        val mockAuthResult = mockk<AuthResult> {
            every { user } returns mockUser
        }
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)

        val websiteUser = User(
            id = "new-uid",
            email = "newuser@test.com",
            name = "New Subscriber",
            tenantId = "tenant-chumbak"
        )
        val authResponse = com.ott.platform.core.api.WebsiteAuthResult(
            user = websiteUser,
            subscription = null
        )
        coEvery { websiteApiClient.register("New Subscriber", "newuser@test.com", "9876543210", "password123") } returns Result.success(authResponse)

        val result = authRepository.register("New Subscriber", "newuser@test.com", "9876543210", "password123")

        assertTrue(result.isSuccess)
        verify { sessionManager.saveSession(websiteUser) }
    }

    @Test
    fun `registerWithEmail returns success on valid input`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        val mockAuthResult = mockk<AuthResult> {
            every { user } returns mockUser
        }
        
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)

        val result = authRepository.registerWithEmail("test@test.com", "password")

        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
    }

    @Test
    fun `signOut clears session and calls firebase auth signOut`() = runTest {
        authRepository.signOut()
        verify { sessionManager.clearSession() }
        verify { firebaseAuth.signOut() }
    }
}
