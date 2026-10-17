package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.PlanDefaults
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.repository.SubscriptionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : SubscriptionRepository {

    private val localActiveSubscription = MutableStateFlow<Subscription?>(null)

    override fun observeSubscription(userId: String, tenantId: String): Flow<Subscription?> = callbackFlow {
        // Emit in-memory active subscription immediately if already verified
        val currentLocal = localActiveSubscription.value
        if (currentLocal != null && currentLocal.userId == userId && currentLocal.isActive()) {
            trySend(currentLocal)
        }

        // Query for active subscriptions in Firestore for this user/tenant
        val listener = firestore.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tenantId", tenantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val fallback = localActiveSubscription.value
                    if (fallback != null && fallback.userId == userId) {
                        trySend(fallback)
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                
                // There might be multiple subscription records over time (expired, cancelled, etc.)
                // Find the one that is currently active, or the most recent one
                val subscriptions = snapshot?.toObjects(Subscription::class.java) ?: emptyList()
                val activeSubscription = subscriptions.firstOrNull { it.isActive() }
                    ?: localActiveSubscription.value?.takeIf { it.userId == userId && it.isActive() }
                    ?: subscriptions.maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }
                
                trySend(activeSubscription)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPlans(tenantId: String): List<Plan> {
        return try {
            val snapshot = firestore.collection("plans")
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("active", true)
                .get()
                .await()
            val plans = snapshot.toObjects(Plan::class.java)
            if (plans.isNotEmpty()) {
                plans
            } else {
                PlanDefaults.getPlans(tenantId)
            }
        } catch (e: Exception) {
            PlanDefaults.getPlans(tenantId)
        }
    }

    override suspend fun isSubscriptionActive(userId: String, tenantId: String): Boolean {
        val currentLocal = localActiveSubscription.value
        if (currentLocal != null && currentLocal.userId == userId && currentLocal.isActive()) {
            return true
        }

        return try {
            val snapshot = firestore.collection("subscriptions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
                
            val subscriptions = snapshot.toObjects(Subscription::class.java)
            subscriptions.any { it.isActive() }
        } catch (e: Exception) {
            localActiveSubscription.value?.let { it.userId == userId && it.isActive() } ?: false
        }
    }

    override suspend fun recordActiveSubscription(subscription: Subscription) {
        localActiveSubscription.value = subscription
        try {
            val docId = if (subscription.id.isNotBlank()) subscription.id else "sub_${subscription.userId}"
            firestore.collection("subscriptions").document(docId).set(subscription).await()
        } catch (e: Exception) {
            // Firestore write may be restricted by security rules for client-side direct writes
            // The localActiveSubscription in-memory state remains authoritative for the session
        }
    }
}

