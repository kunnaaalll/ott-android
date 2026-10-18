package com.ott.platform.data.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<FirebaseUser?>
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun register(name: String, email: String, mobile: String, password: String): Result<FirebaseUser>
    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    suspend fun sendOtp(mobile: String): Result<String>
    suspend fun verifyOtp(mobile: String, otp: String): Result<com.ott.platform.data.model.User>
    suspend fun signOut()
    fun getCurrentUserId(): String?
}
