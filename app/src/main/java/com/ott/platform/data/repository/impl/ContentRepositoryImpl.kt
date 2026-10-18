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
        // 1. Fast initial fetch from authoritative website CMS
        launch {
            val cmsResult = websiteApiClient.fetchContentById(contentId)
            if (cmsResult.isSuccess) {
                trySend(cmsResult.getOrThrow())
            }
        }

        // 2. Real-time updates from Firestore
        val listener = contentCollection.document(contentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val item = snapshot?.toObject(Content::class.java)
                if (item != null) {
                    trySend(item)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun searchContent(tenantId: String, query: String): List<Content> {
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", tenantId)
        val allContent = if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            cmsResult.getOrThrow()
        } else {
            try {
                contentCollection
                    .whereEqualTo("tenantId", tenantId)
                    .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                    .get()
                    .await()
                    .toObjects(Content::class.java)
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
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", tenantId)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            val filtered = cmsResult.getOrThrow().filter { it.categoryIds.contains(categoryId) }
            if (filtered.isNotEmpty()) return filtered
        }

        return try {
            contentCollection
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                .whereArrayContains("categoryIds", categoryId)
                .get()
                .await()
                .toObjects(Content::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getFeaturedContent(tenantId: String): List<Content> {
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", tenantId)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            val items = cmsResult.getOrThrow()
            val featured = items.filter { it.featured }
            return if (featured.isNotEmpty()) featured else items.take(5)
        }

        return try {
            contentCollection
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
                .toObjects(Content::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getNewReleases(tenantId: String, limit: Int): List<Content> {
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", tenantId)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            val items = cmsResult.getOrThrow()
            return items.sortedByDescending { it.releaseDate?.toDate()?.time ?: 0L }.take(limit)
        }

        return try {
            contentCollection
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                .orderBy("releaseDate", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(Content::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTrendingContent(tenantId: String, limit: Int): List<Content> {
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", tenantId)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            return cmsResult.getOrThrow().take(limit)
        }

        return try {
            contentCollection
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(Content::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPopularContent(tenantId: String, limit: Int): List<Content> {
        val cmsResult = websiteApiClient.fetchCatalog("ANDROID", tenantId)
        if (cmsResult.isSuccess && cmsResult.getOrThrow().isNotEmpty()) {
            return cmsResult.getOrThrow().reversed().take(limit)
        }

        return try {
            contentCollection
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("status", ContentStatus.PUBLISHED.name)
                .orderBy("title", Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(Content::class.java)
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
