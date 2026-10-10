package com.ott.platform.data.repository.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
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
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        authRepository = AuthRepositoryImpl(firebaseAuth)
    }

    @Test
    fun `signInWithEmail returns success on valid credentials`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        val mockAuthResult = mockk<AuthResult> {
            every { user } returns mockUser
        }
        
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)

        val result = authRepository.signInWithEmail("test@test.com", "password")

        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
    }

    @Test
    fun `signInWithEmail returns failure on error`() = runTest {
        val exception = Exception("Network error")
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)

        val result = authRepository.signInWithEmail("test@test.com", "password")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
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
    fun `signOut calls firebase auth signOut`() = runTest {
        authRepository.signOut()
        verify { firebaseAuth.signOut() }
    }
}
