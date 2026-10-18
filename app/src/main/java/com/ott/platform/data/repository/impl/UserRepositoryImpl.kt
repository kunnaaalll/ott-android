package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ott.platform.core.session.SessionManager
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
    private val sessionManager: SessionManager,
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override fun observeUser(uid: String): Flow<User?> = callbackFlow {
        // Emit cached session user immediately for zero-latency UI consistency
        val cached = sessionManager.getSessionUser()
        if (cached != null && (cached.id == uid || uid.isBlank())) {
            trySend(cached)
        }

        val effectiveUid = if (uid.isNotBlank()) uid else cached?.id ?: ""
        if (effectiveUid.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val listener = usersCollection.document(effectiveUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val fallback = sessionManager.getSessionUser()
                    if (fallback != null && (fallback.id == effectiveUid || effectiveUid.isBlank())) {
                        trySend(fallback)
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                val remoteUser = snapshot?.toObject(User::class.java)
                trySend(remoteUser ?: sessionManager.getSessionUser())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createOrUpdateUser(user: User) {
        sessionManager.saveSession(user)
        try {
            kotlinx.coroutines.withTimeoutOrNull(1200L) {
                val docRef = usersCollection.document(user.id)
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val updates = mutableMapOf<String, Any>(
                        "email" to user.email,
                        "name" to user.name,
                        "photoUrl" to user.photoUrl
                    )
                    docRef.update(updates).await()
                } else {
                    docRef.set(user).await()
                }
            }
        } catch (e: Exception) {
            // Local session is saved and authoritative
        }
    }

    override suspend fun updateLastLogin(uid: String) {
        try {
            kotlinx.coroutines.withTimeoutOrNull(1200L) {
                usersCollection.document(uid).update("lastLoginAt", FieldValue.serverTimestamp()).await()
            }
        } catch (e: Exception) {
            // Ignored if offline
        }
    }

    // ── My List ────────────────────────────────────────────────────

    private fun myListCollection(uid: String) = usersCollection.document(uid).collection("myList")

    override fun observeMyList(uid: String): Flow<List<String>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = myListCollection(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val contentIds = snapshot?.documents?.map { it.id } ?: emptyList()
                trySend(contentIds)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addToMyList(uid: String, contentId: String) {
        try {
            val data = hashMapOf(
                "addedAt" to FieldValue.serverTimestamp()
            )
            myListCollection(uid).document(contentId).set(data).await()
        } catch (e: Exception) {
            // Ignored
        }
    }

    override suspend fun removeFromMyList(uid: String, contentId: String) {
        try {
            myListCollection(uid).document(contentId).delete().await()
        } catch (e: Exception) {
            // Ignored
        }
    }

    override suspend fun isInMyList(uid: String, contentId: String): Boolean {
        return try {
            val snapshot = myListCollection(uid).document(contentId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }
}
