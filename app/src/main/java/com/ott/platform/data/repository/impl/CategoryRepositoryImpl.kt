package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ott.platform.data.model.Category
import com.ott.platform.data.repository.CategoryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CategoryRepository {
    
    private val categoriesCollection = firestore.collection("categories")

    override fun observeActiveCategories(tenantId: String): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("active", true)
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Category::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun observeAllCategories(tenantId: String): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection
            .whereEqualTo("tenantId", tenantId)
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Category::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getCategoryById(categoryId: String): Category? {
        val snapshot = categoriesCollection.document(categoryId).get().await()
        return snapshot.toObject(Category::class.java)
    }

    override suspend fun saveCategory(category: Category) {
        val docRef = if (category.id.isEmpty()) categoriesCollection.document() else categoriesCollection.document(category.id)
        docRef.set(category).await()
    }

    override suspend fun deleteCategory(categoryId: String) {
        // Soft delete/archive
        categoriesCollection.document(categoryId).update("active", false).await()
    }
}
