package com.ott.platform.data.repository.impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.core.session.SessionManager
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.SubscriptionRepository
import com.ott.platform.data.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val websiteApiClient: WebsiteApiClient,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository
) : AuthRepository {

    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        val trimmedIdentifier = email.trim()

        // 1. Authoritative login verification with Website backend (thechumbak.com/api/auth/login)
        val websiteResult = websiteApiClient.login(trimmedIdentifier, password)
        if (websiteResult.isSuccess) {
            val authData = websiteResult.getOrThrow()
            val user = authData.user
            
            // Sync user profile & active subscription to local storage
            sessionManager.saveSession(user)
            userRepository.createOrUpdateUser(user)
            if (authData.subscription != null) {
                subscriptionRepository.recordActiveSubscription(authData.subscription)
            }

            // Sync with Firebase Auth if available
            val firebaseEmail = if (user.email.contains("@")) user.email else "$trimmedIdentifier@thechumbak.com"
            try {
                val res = auth.signInWithEmailAndPassword(firebaseEmail, password).await()
                res.user?.let { return Result.success(it) }
            } catch (e: Exception) {
                try {
                    val res = auth.createUserWithEmailAndPassword(firebaseEmail, password).await()
                    res.user?.let { return Result.success(it) }
                } catch (ce: Exception) {
                    // Firebase Auth might not be enabled or configured; website login is already authenticated
                }
            }

            // If Firebase user is available, return it; otherwise return website auth success indicator
            val currentFb = auth.currentUser
            if (currentFb != null) {
                return Result.success(currentFb)
            }
            try {
                val anonResult = auth.signInAnonymously().await()
                anonResult.user?.let { return Result.success(it) }
            } catch (e: Exception) {
                // Ignore anonymous auth failure
            }

            // Return custom marker for website authenticated session
            return Result.failure(Exception("WEBSITE_AUTH_SUCCESS"))
        }

        // 2. Direct Firebase Auth fallback only if identifier contains '@'
        if (trimmedIdentifier.contains("@")) {
            return try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Login failed: User is null")
                sessionManager.saveSession(
                    User(
                        id = user.uid,
                        email = user.email ?: email,
                        name = user.displayName ?: "",
                        tenantId = "tenant_chumbak"
                    )
                )
                Result.success(user)
            } catch (e: Exception) {
                val websiteError = websiteResult.exceptionOrNull()?.message
                if (!websiteError.isNullOrBlank()) {
                    Result.failure(Exception(websiteError))
                } else {
                    Result.failure(e)
                }
            }
        } else {
            val websiteError = websiteResult.exceptionOrNull()?.message ?: "Invalid credentials"
            return Result.failure(Exception(websiteError))
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        mobile: String,
        password: String
    ): Result<FirebaseUser> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim().ifBlank { "Subscriber" }
        val cleanMobile = mobile.replace(Regex("[^0-9]"), "").takeLast(10)

        // 1. Authoritative registration on Website backend (thechumbak.com/api/auth/register)
        val websiteResult = websiteApiClient.register(trimmedName, trimmedEmail, cleanMobile, password)
        if (websiteResult.isSuccess) {
            val authData = websiteResult.getOrThrow()
            sessionManager.saveSession(authData.user)
            userRepository.createOrUpdateUser(authData.user)
        }

        // 2. Register in Firebase Auth
        return try {
            val result = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val user = result.user ?: throw Exception("Registration failed: User is null")
            Result.success(user)
        } catch (e: Exception) {
            if (sessionManager.isLoggedIn() && auth.currentUser != null) {
                Result.success(auth.currentUser!!)
            } else {
                Result.failure(websiteResult.exceptionOrNull() ?: e)
            }
        }
    }

    override suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser> {
        val trimmedEmail = email.trim().lowercase()
        val name = trimmedEmail.substringBefore("@").replace(".", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        return register(name, trimmedEmail, "9999999999", password)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google Sign-In failed: User is null")
            sessionManager.saveSession(
                User(
                    id = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    tenantId = "tenant_chumbak"
                )
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendOtp(mobile: String): Result<String> {
        return websiteApiClient.sendOtp(mobile)
    }

    override suspend fun verifyOtp(mobile: String, otp: String): Result<User> {
        val result = websiteApiClient.verifyOtp(mobile, otp)
        if (result.isSuccess) {
            val authData = result.getOrThrow()
            val user = authData.user
            sessionManager.saveSession(user)
            userRepository.createOrUpdateUser(user)
            if (authData.subscription != null) {
                subscriptionRepository.recordActiveSubscription(authData.subscription)
            }
            return Result.success(user)
        }
        return Result.failure(result.exceptionOrNull() ?: Exception("Failed to verify OTP"))
    }

    override suspend fun signOut() {
        sessionManager.clearSession()
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Graceful logout
        }
    }
    
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid ?: sessionManager.getUserId()
    }
}
