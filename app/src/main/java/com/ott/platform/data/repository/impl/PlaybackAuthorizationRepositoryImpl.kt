package com.ott.platform.data.repository.impl

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.ott.platform.data.model.PlaybackAuthorization
import com.ott.platform.data.repository.PlaybackAuthorizationRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackAuthorizationRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions
) : PlaybackAuthorizationRepository {

    override suspend fun authorizePlayback(contentId: String, isTrailer: Boolean): Result<PlaybackAuthorization> {
        return try {
            val result = functions
                .getHttpsCallable("authorizePlayback")
                .call(mapOf("contentId" to contentId, "isTrailer" to isTrailer))
                .await()
            
            val data = result.data as? Map<String, Any>
                ?: return Result.failure(Exception("Invalid response format"))
                
            val authorized = data["authorized"] as? Boolean ?: false
            val playbackUrl = data["playbackUrl"] as? String ?: ""
            val expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L
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
