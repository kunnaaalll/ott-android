package com.ott.platform.data.repository

import kotlinx.coroutines.flow.Flow

interface MyListRepository {
    /** Observe all content IDs in the user's My List */
    fun observeMyListIds(userId: String): Flow<List<String>>

    /** Check if a specific content ID is in the user's My List */
    fun observeIsInMyList(userId: String, contentId: String): Flow<Boolean>

    /** Add content to My List */
    suspend fun addToMyList(userId: String, contentId: String)

    /** Remove content from My List */
    suspend fun removeFromMyList(userId: String, contentId: String)
}
