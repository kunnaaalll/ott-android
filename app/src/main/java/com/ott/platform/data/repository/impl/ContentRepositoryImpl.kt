package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.data.model.Content
import com.ott.platform.data.model.ContentStatus
import com.ott.platform.data.repository.ContentRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val websiteApiClient: WebsiteApiClient
) : ContentRepository {

    private val contentCollection = firestore.collection("content")

    override fun observeContent(tenantId: String): Flow<List<Content>> = callbackFlow {
        // 1. Fast initial emit directly from authoritative website CMS
        launch {
            val cmsResult = websiteApiClient.fetchCatalog("ANDROID", tenantId)
            if (cmsResult.isSuccess) {
                val items = cmsResult.getOrThrow()
                if (items.isNotEmpty()) {
                    trySend(items)
                }
            }
        }

        // 2. Real-time updates from Firestore
        val listener = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", ContentStatus.PUBLISHED.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val content = snapshot?.toObjects(Content::class.java) ?: emptyList()
                if (content.isNotEmpty()) {
                    trySend(content)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeContentById(contentId: String): Flow<Content?> = callbackFlow {
        if (contentId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        // 1. Fast initial fetch from authoritative website CMS
        launch {
            try {
                val cmsResult = websiteApiClient.fetchContentById(contentId)
                if (cmsResult.isSuccess) {
                    trySend(cmsResult.getOrThrow())
                }
            } catch (e: Exception) {
                // Ignore CMS fetch failure
            }
        }

        // 2. Real-time updates from Firestore (safely registered)
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = contentCollection.document(contentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    try {
                        val item = snapshot?.toObject(Content::class.java)
                        if (item != null) {
                            trySend(item)
                        }
                    } catch (e: Exception) {
                        // Serialization failure protection
                    }
                }
        } catch (e: Exception) {
            // Document path error protection
        }
        awaitClose { listener?.remove() }
    }

    private fun resolveTenant(tenantId: String): String {
        return if (tenantId.isBlank() || tenantId == "default-tenant") "tenant_chumbak" else tenantId
    }

    override suspend fun searchContent(tenantId: String, query: String): List<Content> {
        val effectiveTenant = resolveTenant(tenantId)
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", effectiveTenant)
        val allContent = if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            cmsResult.getOrThrow()
        } else {
            try {
                kotlinx.coroutines.withTimeoutOrNull(1500L) {
                    contentCollection
                        .whereEqualTo("tenantId", effectiveTenant)
                        .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                        .get()
                        .await()
                        .toObjects(Content::class.java)
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val lowerQuery = query.lowercase()
        return allContent.filter {
            it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
        }
    }

    override suspend fun getContentByCategory(tenantId: String, categoryId: String): List<Content> {
        val effectiveTenant = resolveTenant(tenantId)
        val apiResult = websiteApiClient.fetchByGenre(categoryId, effectiveTenant)
        if (apiResult.isSuccess && apiResult.getOrThrow().isNotEmpty()) {
            return apiResult.getOrThrow()
        }

        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", effectiveTenant)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            val target = categoryId.lowercase().replace("cat_", "")
            val filtered = cmsResult.getOrThrow().filter { item ->
                item.isPublished() && item.categoryIds.any { cat ->
                    val c = cat.lowercase()
                    c == target || c == "cat_$target" || c == categoryId.lowercase()
                }
            }
            if (filtered.isNotEmpty()) {
                return filtered.sortedWith(compareByDescending<Content> { it.getEffectivePublishTime() }.thenByDescending { it.id })
            }
        }

        return try {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                contentCollection
                    .whereEqualTo("tenantId", effectiveTenant)
                    .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                    .whereArrayContains("categoryIds", categoryId)
                    .get()
                    .await()
                    .toObjects(Content::class.java)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getContentByLanguage(tenantId: String, language: String): List<Content> {
        val effectiveTenant = resolveTenant(tenantId)
        val apiResult = websiteApiClient.fetchByLanguage(language, effectiveTenant)
        if (apiResult.isSuccess && apiResult.getOrThrow().isNotEmpty()) {
            return apiResult.getOrThrow()
        }

        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", effectiveTenant)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            val filtered = cmsResult.getOrThrow().filter { item ->
                item.isPublished() && item.language.equals(language, ignoreCase = true)
            }
            if (filtered.isNotEmpty()) {
                return filtered.sortedWith(compareByDescending<Content> { it.getEffectivePublishTime() }.thenByDescending { it.id })
            }
        }

        return try {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                contentCollection
                    .whereEqualTo("tenantId", effectiveTenant)
                    .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                    .whereEqualTo("language", language)
                    .get()
                    .await()
                    .toObjects(Content::class.java)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getFeaturedContent(tenantId: String): List<Content> {
        val effectiveTenant = resolveTenant(tenantId)
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", effectiveTenant)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            val items = cmsResult.getOrThrow().filter { it.isPublished() }
            val featured = items.filter { it.featured }
            return if (featured.isNotEmpty()) featured else items.take(5)
        }

        return try {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                contentCollection
                    .whereEqualTo("tenantId", effectiveTenant)
                    .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()
                    .toObjects(Content::class.java)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getNewReleases(tenantId: String, limit: Int): List<Content> {
        val effectiveTenant = resolveTenant(tenantId)
        val apiResult = websiteApiClient.fetchNewReleases(limit, effectiveTenant)
        if (apiResult.isSuccess && apiResult.getOrThrow().isNotEmpty()) {
            return apiResult.getOrThrow()
        }

        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", effectiveTenant)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            return cmsResult.getOrThrow()
                .filter { it.isPublished() }
                .sortedWith(compareByDescending<Content> { it.getEffectivePublishTime() }.thenByDescending { it.id })
                .take(limit)
        }

        return try {
            val list = kotlinx.coroutines.withTimeoutOrNull(1500L) {
                contentCollection
                    .whereEqualTo("tenantId", effectiveTenant)
                    .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                    .orderBy("publishedAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                    .toObjects(Content::class.java)
            }
            if (!list.isNullOrEmpty()) {
                list
            } else {
                kotlinx.coroutines.withTimeoutOrNull(1500L) {
                    contentCollection
                        .whereEqualTo("tenantId", effectiveTenant)
                        .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limit.toLong())
                        .get()
                        .await()
                        .toObjects(Content::class.java)
                } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTrendingContent(tenantId: String, limit: Int): List<Content> {
        val effectiveTenant = resolveTenant(tenantId)
        val apiResult = websiteApiClient.fetchTrending(limit, effectiveTenant)
        if (apiResult.isSuccess && apiResult.getOrThrow().isNotEmpty()) {
            return apiResult.getOrThrow()
        }

        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", effectiveTenant)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            val trending = cmsResult.getOrThrow().filter { it.isPublished() && it.isTrending }
            if (trending.isNotEmpty()) {
                return trending.sortedWith(compareByDescending<Content> { it.getEffectivePublishTime() }.thenByDescending { it.id }).take(limit)
            }
            return cmsResult.getOrThrow()
                .filter { it.isPublished() }
                .sortedWith(compareByDescending<Content> { it.getEffectivePublishTime() }.thenByDescending { it.id })
                .take(limit)
        }

        return try {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                contentCollection
                    .whereEqualTo("tenantId", effectiveTenant)
                    .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                    .whereEqualTo("isTrending", true)
                    .limit(limit.toLong())
                    .get()
                    .await()
                    .toObjects(Content::class.java)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPopularContent(tenantId: String, limit: Int): List<Content> {
        val effectiveTenant = resolveTenant(tenantId)
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", effectiveTenant)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            return cmsResult.getOrThrow().reversed().take(limit)
        }

        return try {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                contentCollection
                    .whereEqualTo("tenantId", effectiveTenant)
                    .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                    .orderBy("title", Query.Direction.ASCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                    .toObjects(Content::class.java)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Admin Operations ──────────────────────────────────────────

    override fun observeAllContentAdmin(tenantId: String): Flow<List<Content>> = callbackFlow {
        val listener = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val content = snapshot?.toObjects(Content::class.java) ?: emptyList()
                trySend(content)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun searchAllContentAdmin(tenantId: String, query: String): List<Content> {
        val snapshot = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()

        val allContent = snapshot.toObjects(Content::class.java)
        val lowerQuery = query.lowercase()
        return allContent.filter {
            it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
        }
    }

    override suspend fun getSeriesHierarchy(seriesId: String): com.ott.platform.data.model.SeriesHierarchy? {
        val result = websiteApiClient.fetchSeriesHierarchy(seriesId)
        if (result.isSuccess) {
            return result.getOrNull()
        }
        return null
    }

    override suspend fun saveContent(content: Content) {
        val docRef = if (content.id.isEmpty()) contentCollection.document() else contentCollection.document(content.id)
        docRef.set(content).await()
    }

    override suspend fun updateContentStatus(contentId: String, status: String) {
        val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
        val data = hashMapOf(
            "contentId" to contentId,
            "status" to status
        )
        functions.getHttpsCallable("changeContentStatus").call(data).await()
    }
}
