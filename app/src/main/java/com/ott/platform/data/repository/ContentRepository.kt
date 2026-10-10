package com.ott.platform.data.repository

import com.ott.platform.data.model.Content
import kotlinx.coroutines.flow.Flow

/**
 * Content catalogue operations.
 * Reads content from Firestore filtered by the current tenant.
 */
interface ContentRepository {

    /** Observe all published content for the tenant. */
    fun observeContent(tenantId: String): Flow<List<Content>>

    /** Observe a single content item by ID. */
    fun observeContentById(contentId: String): Flow<Content?>

    /** Search content by title. */
    suspend fun searchContent(tenantId: String, query: String): List<Content>

    /** Get content by category. */
    suspend fun getContentByCategory(tenantId: String, categoryId: String): List<Content>

    /** Get featured content (for hero section). */
    suspend fun getFeaturedContent(tenantId: String): List<Content>

    /** Get recently added content. */
    suspend fun getNewReleases(tenantId: String, limit: Int = 20): List<Content>
    
    /** Get trending content. */
    suspend fun getTrendingContent(tenantId: String, limit: Int = 20): List<Content>
    
    /** Get popular content. */
    suspend fun getPopularContent(tenantId: String, limit: Int = 20): List<Content>

    // ── Admin Operations ──────────────────────────────────────────

    /** Observe all content (including drafts and archived) for admin view. */
    fun observeAllContentAdmin(tenantId: String): Flow<List<Content>>

    /** Search all content (including drafts) by title. */
    suspend fun searchAllContentAdmin(tenantId: String, query: String): List<Content>

    /** Save (create or update) content. */
    suspend fun saveContent(content: Content)

    /** Change content status (e.g. Publish, Archive, Draft) */
    suspend fun updateContentStatus(contentId: String, status: String)
}
