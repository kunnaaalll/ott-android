package com.ott.platform.data.repository

import com.ott.platform.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * User profile operations.
 * User documents are created on first login and linked to a tenant.
 */
interface UserRepository {

    /** Observe the current user's profile. */
    fun observeUser(uid: String): Flow<User?>

    /** Create or update user profile in Firestore after auth. */
    suspend fun createOrUpdateUser(user: User)

    /** Update last login timestamp. */
    suspend fun updateLastLogin(uid: String)

    // ── My List ────────────────────────────────────────────────────

    /** Get IDs of content items in the user's list. */
    fun observeMyList(uid: String): Flow<List<String>>

    /** Add a content ID to the user's list. */
    suspend fun addToMyList(uid: String, contentId: String)

    /** Remove a content ID from the user's list. */
    suspend fun removeFromMyList(uid: String, contentId: String)

    /** Check if a content item is in the user's list. */
    suspend fun isInMyList(uid: String, contentId: String): Boolean
}
