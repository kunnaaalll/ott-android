package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.repository.SubscriptionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : SubscriptionRepository {

    override fun observeSubscription(userId: String, tenantId: String): Flow<Subscription?> = callbackFlow {
        // Query for active subscriptions for this user/tenant
        val listener = firestore.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tenantId", tenantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                // There might be multiple subscription records over time (expired, cancelled, etc.)
                // Find the one that is currently active, or the most recent one
                val subscriptions = snapshot?.toObjects(Subscription::class.java) ?: emptyList()
                val activeSubscription = subscriptions.firstOrNull { it.isActive() }
                    ?: subscriptions.maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }
                
                trySend(activeSubscription)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPlans(tenantId: String): List<Plan> {
        val snapshot = firestore.collection("plans")
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("active", true)
            .get()
            .await()
        return snapshot.toObjects(Plan::class.java)
    }

    override suspend fun isSubscriptionActive(userId: String, tenantId: String): Boolean {
        val snapshot = firestore.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
            
        val subscriptions = snapshot.toObjects(Subscription::class.java)
        return subscriptions.any { it.isActive() }
    }
}
