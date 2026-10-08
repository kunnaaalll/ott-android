package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createOrUpdateUser(user: User) {
        val docRef = usersCollection.document(user.id)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            // Update existing user (e.g., photoUrl or name changes)
            val updates = mutableMapOf<String, Any>(
                "email" to user.email,
                "name" to user.name,
                "photoUrl" to user.photoUrl
            )
            docRef.update(updates).await()
        } else {
            // Create new user
            docRef.set(user).await()
        }
    }

    override suspend fun updateLastLogin(uid: String) {
        usersCollection.document(uid).update("lastLoginAt", FieldValue.serverTimestamp()).await()
    }

    // ── My List ────────────────────────────────────────────────────

    private fun myListCollection(uid: String) = usersCollection.document(uid).collection("myList")

    override fun observeMyList(uid: String): Flow<List<String>> = callbackFlow {
        val listener = myListCollection(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val contentIds = snapshot?.documents?.map { it.id } ?: emptyList()
                trySend(contentIds)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addToMyList(uid: String, contentId: String) {
        val data = hashMapOf(
            "addedAt" to FieldValue.serverTimestamp()
        )
        myListCollection(uid).document(contentId).set(data).await()
    }

    override suspend fun removeFromMyList(uid: String, contentId: String) {
        myListCollection(uid).document(contentId).delete().await()
    }

    override suspend fun isInMyList(uid: String, contentId: String): Boolean {
        val snapshot = myListCollection(uid).document(contentId).get().await()
        return snapshot.exists()
    }
}
