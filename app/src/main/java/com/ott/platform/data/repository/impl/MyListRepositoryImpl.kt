package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ott.platform.data.model.MyListItem
import com.ott.platform.data.repository.MyListRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyListRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MyListRepository {

    private fun userMyListCollection(userId: String) =
        firestore.collection("users").document(userId).collection("myList")

    override fun observeMyListIds(userId: String): Flow<List<String>> = callbackFlow {
        val listener = userMyListCollection(userId)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(MyListItem::class.java) ?: emptyList()
                trySend(items.map { it.contentId })
            }
        awaitClose { listener.remove() }
    }

    override fun observeIsInMyList(userId: String, contentId: String): Flow<Boolean> = callbackFlow {
        val listener = userMyListCollection(userId).document(contentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() ?: false)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addToMyList(userId: String, contentId: String) {
        val item = mapOf(
            "contentId" to contentId,
            "addedAt" to FieldValue.serverTimestamp()
        )
        userMyListCollection(userId).document(contentId).set(item).await()
    }

    override suspend fun removeFromMyList(userId: String, contentId: String) {
        userMyListCollection(userId).document(contentId).delete().await()
    }
}
