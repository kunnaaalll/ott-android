package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ott.platform.data.model.Content
import com.ott.platform.data.model.ContentStatus
import com.ott.platform.data.repository.ContentRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ContentRepository {

    private val contentCollection = firestore.collection("content")

    override fun observeContent(tenantId: String): Flow<List<Content>> = callbackFlow {
        val listener = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", ContentStatus.PUBLISHED.name)
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

    override fun observeContentById(contentId: String): Flow<Content?> = callbackFlow {
        val listener = contentCollection.document(contentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Content::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun searchContent(tenantId: String, query: String): List<Content> {
        // Firestore doesn't support full-text search natively.
        // For MVP, we fetch all published content and filter client-side.
        // In production, use Algolia/Typesense or a Cloud Function.
        val snapshot = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", ContentStatus.PUBLISHED.name)
            .get()
            .await()

        val allContent = snapshot.toObjects(Content::class.java)
        val lowerQuery = query.lowercase()
        return allContent.filter {
            it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
        }
    }

    override suspend fun getContentByCategory(tenantId: String, categoryId: String): List<Content> {
        val snapshot = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", ContentStatus.PUBLISHED.name)
            .whereArrayContains("categoryIds", categoryId)
            .get()
            .await()
        return snapshot.toObjects(Content::class.java)
    }

    override suspend fun getFeaturedContent(tenantId: String): List<Content> {
        // For MVP, return the most recent published content.
        // In the future, a "featured" flag or admin-managed list can be added.
        val snapshot = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", ContentStatus.PUBLISHED.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .await()
        return snapshot.toObjects(Content::class.java)
    }

    override suspend fun getNewReleases(tenantId: String, limit: Int): List<Content> {
        val snapshot = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", ContentStatus.PUBLISHED.name)
            .orderBy("releaseDate", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        return snapshot.toObjects(Content::class.java)
    }

    override suspend fun getTrendingContent(tenantId: String, limit: Int): List<Content> {
        // MVP: Order by updatedAt or title for variation
        val snapshot = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", ContentStatus.PUBLISHED.name)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        return snapshot.toObjects(Content::class.java)
    }

    override suspend fun getPopularContent(tenantId: String, limit: Int): List<Content> {
        // MVP: Another variation
        val snapshot = contentCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", ContentStatus.PUBLISHED.name)
            .orderBy("title", Query.Direction.ASCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        return snapshot.toObjects(Content::class.java)
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
