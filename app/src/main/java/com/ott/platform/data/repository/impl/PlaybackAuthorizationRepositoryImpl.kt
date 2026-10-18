package com.ott.platform.data.repository.impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.core.session.SessionManager
import com.ott.platform.data.model.PlaybackAuthorization
import com.ott.platform.data.repository.PlaybackAuthorizationRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackAuthorizationRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val websiteApiClient: WebsiteApiClient,
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) : PlaybackAuthorizationRepository {

    override suspend fun authorizePlayback(
        contentId: String,
        isTrailer: Boolean,
        seriesId: String?,
        seasonId: String?
    ): Result<PlaybackAuthorization> {
        val currentUserId = auth.currentUser?.uid ?: sessionManager.getUserId()

        // Zero-Leakage: Without logging in, streaming cannot happen
        if (currentUserId.isNullOrBlank()) {
            return Result.failure(Exception("UNAUTHENTICATED"))
        }

        // 1. Authoritative verification via Website API (/api/playback/authorize)
        val websiteResult = websiteApiClient.authorizePlayback(
            userId = currentUserId,
            contentId = contentId,
            isTrailer = isTrailer,
            seriesId = seriesId,
            seasonId = seasonId
        )
        if (websiteResult.isSuccess) {
            return websiteResult
        } else {
            val exMsg = websiteResult.exceptionOrNull()?.message
            if (exMsg == "UNAUTHENTICATED" || exMsg == "SUBSCRIPTION_REQUIRED") {
                return websiteResult
            }
        }

        // 2. Fallback to Firebase Functions if website API is unreachable
        return try {
            val payload = mutableMapOf<String, Any>(
                "contentId" to contentId,
                "isTrailer" to isTrailer
            )
            if (seriesId != null) payload["seriesId"] = seriesId
            if (seasonId != null) payload["seasonId"] = seasonId

            val result = functions
                .getHttpsCallable("authorizePlayback")
                .call(payload)
                .await()
            
            val data = result.data as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response format"))
                
            val authorized = data["authorized"] as? Boolean ?: false
            val playbackUrl = data["playbackUrl"] as? String ?: ""
            val expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L
            @Suppress("UNCHECKED_CAST")
            val cookiesMap = data["cookies"] as? Map<String, String> ?: emptyMap()
            val reason = data["reason"] as? String

            Result.success(
                PlaybackAuthorization(
                    authorized = authorized,
                    playbackUrl = playbackUrl,
                    expiresAt = expiresAt,
                    cookies = cookiesMap,
                    reason = reason
                )
            )
        } catch (e: Exception) {
            val reason = if (e is FirebaseFunctionsException) {
                when (e.code) {
                    FirebaseFunctionsException.Code.UNAUTHENTICATED -> "UNAUTHENTICATED"
                    FirebaseFunctionsException.Code.NOT_FOUND -> "CONTENT_UNAVAILABLE"
                    FirebaseFunctionsException.Code.PERMISSION_DENIED -> "SUBSCRIPTION_REQUIRED"
                    else -> "SERVER_ERROR"
                }
            } else {
                "UNKNOWN_ERROR"
            }
            Result.failure(Exception(reason))
        }
    }
}
