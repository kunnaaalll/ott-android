package com.ott.platform.data.repository.impl

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.PlanDefaults
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.SubscriptionStatus
import com.ott.platform.data.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context? = null,
    private val websiteApiClient: com.ott.platform.core.api.WebsiteApiClient? = null,
    private val sessionManager: com.ott.platform.core.session.SessionManager? = null
) : SubscriptionRepository {

    private val prefs = context?.getSharedPreferences("ott_subscription_cache", Context.MODE_PRIVATE)

    private fun loadCachedSubscription(): Subscription? {
        val raw = prefs?.getString("cached_active_sub", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val startMs = json.optLong("currentPeriodStart", 0L)
            val endMs = json.optLong("currentPeriodEnd", 0L)
            Subscription(
                id = json.optString("id", ""),
                userId = json.optString("userId", ""),
                tenantId = json.optString("tenantId", "tenant_chumbak"),
                planId = json.optString("planId", "plan_year_vip"),
                status = json.optString("status", SubscriptionStatus.ACTIVE.name),
                currentPeriodStart = startMs,
                currentPeriodEnd = endMs,
                provider = json.optString("provider", "CASHFREE_PROD"),
                providerSubscriptionId = json.optString("providerSubscriptionId", ""),
                startDate = Timestamp(Date(startMs)),
                expiryDate = Timestamp(Date(endMs))
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCachedSubscription(subscription: Subscription?) {
        if (subscription == null || subscription.status != SubscriptionStatus.ACTIVE.name || !subscription.isActive()) {
            prefs?.edit()?.remove("cached_active_sub")?.apply()
            return
        }
        try {
            val json = JSONObject().apply {
                put("id", subscription.id)
                put("userId", subscription.userId)
                put("tenantId", subscription.tenantId)
                put("planId", subscription.planId)
                put("status", subscription.status)
                put("currentPeriodStart", subscription.currentPeriodStart ?: (subscription.startDate?.toDate()?.time ?: System.currentTimeMillis()))
                put("currentPeriodEnd", subscription.currentPeriodEnd ?: (subscription.expiryDate?.toDate()?.time ?: (System.currentTimeMillis() + 365L * 86400000L)))
                put("provider", subscription.provider)
                put("providerSubscriptionId", subscription.providerSubscriptionId)
            }
            prefs?.edit()?.putString("cached_active_sub", json.toString())?.apply()
        } catch (e: Exception) {
            // Ignore caching error
        }
    }

    private val localActiveSubscription = MutableStateFlow<Subscription?>(loadCachedSubscription())

    override fun observeSubscription(userId: String, tenantId: String): Flow<Subscription?> = callbackFlow {
        // 1. Emit cached/in-memory active subscription immediately
        val currentLocal = localActiveSubscription.value ?: loadCachedSubscription()
        if (currentLocal != null && currentLocal.isActive()) {
            trySend(currentLocal)
        }

        // 2. Query authoritative server state asynchronously
        val syncJob = launch(Dispatchers.IO) {
            try {
                val user = sessionManager?.getSessionUser()
                val email = user?.email ?: ""
                val mobile = sessionManager?.getPhoneNumber() ?: ""
                val targetId = if (userId.isNotBlank()) userId else (user?.id ?: "")

                val response = websiteApiClient?.fetchSubscription(targetId, email, mobile)
                if (response != null && response.isSuccess) {
                    val remoteSub = response.getOrNull()
                    if (remoteSub != null && remoteSub.isActive()) {
                        localActiveSubscription.value = remoteSub
                        saveCachedSubscription(remoteSub)
                        trySend(remoteSub)
                    } else if (remoteSub != null && !remoteSub.isActive()) {
                        localActiveSubscription.value = remoteSub
                        saveCachedSubscription(remoteSub)
                        trySend(remoteSub)
                    }
                }
            } catch (e: Exception) {
                // Non-fatal sync
            }
        }

        // 3. Query for active subscriptions in Firestore for this user/tenant
        val listener = firestore.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tenantId", tenantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val fallback = localActiveSubscription.value ?: loadCachedSubscription()
                    trySend(fallback)
                    return@addSnapshotListener
                }
                
                val subscriptions = snapshot?.toObjects(Subscription::class.java) ?: emptyList()
                val activeSubscription = subscriptions.firstOrNull { it.isActive() }
                    ?: (localActiveSubscription.value ?: loadCachedSubscription())?.takeIf { it.isActive() }
                    ?: subscriptions.maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }
                
                if (activeSubscription != null && activeSubscription.isActive()) {
                    localActiveSubscription.value = activeSubscription
                    saveCachedSubscription(activeSubscription)
                }
                trySend(activeSubscription)
            }
        awaitClose { 
            syncJob.cancel()
            listener.remove() 
        }
    }

    override suspend fun getPlans(tenantId: String): List<Plan> {
        return try {
            // 1. Authoritative dynamic plans from backend (remotely controllable without app updates)
            val remotePlans = websiteApiClient?.fetchPlans(tenantId)
            if (remotePlans != null && remotePlans.isSuccess && remotePlans.getOrNull()?.isNotEmpty() == true) {
                return remotePlans.getOrNull()!!
            }

            // 2. Firestore fallback if backend is unreachable
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
        val currentLocal = localActiveSubscription.value ?: loadCachedSubscription()
        if (currentLocal != null && currentLocal.isActive()) {
            return true
        }

        // Check authoritative website endpoint
        if (websiteApiClient != null) {
            try {
                val user = sessionManager?.getSessionUser()
                val email = user?.email ?: ""
                val mobile = sessionManager?.getPhoneNumber() ?: ""
                val targetId = if (userId.isNotBlank()) userId else (user?.id ?: "")
                val res = websiteApiClient.fetchSubscription(targetId, email, mobile)
                if (res.isSuccess) {
                    val remoteSub = res.getOrNull()
                    if (remoteSub != null && remoteSub.isActive()) {
                        localActiveSubscription.value = remoteSub
                        saveCachedSubscription(remoteSub)
                        return true
                    }
                }
            } catch (e: Exception) {
                // Non-fatal
            }
        }

        return try {
            val snapshot = firestore.collection("subscriptions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
                
            val subscriptions = snapshot.toObjects(Subscription::class.java)
            val anyActive = subscriptions.any { it.isActive() }
            if (anyActive) {
                val active = subscriptions.firstOrNull { it.isActive() }
                if (active != null) {
                    localActiveSubscription.value = active
                    saveCachedSubscription(active)
                }
            }
            anyActive
        } catch (e: Exception) {
            val fallback = localActiveSubscription.value ?: loadCachedSubscription()
            fallback?.isActive() ?: false
        }
    }

    override suspend fun recordActiveSubscription(subscription: Subscription) {
        localActiveSubscription.value = subscription
        saveCachedSubscription(subscription)
        try {
            kotlinx.coroutines.withTimeoutOrNull(1200L) {
                val docId = if (subscription.id.isNotBlank()) subscription.id else "sub_${subscription.userId}"
                firestore.collection("subscriptions").document(docId).set(subscription).await()
            }
        } catch (e: Exception) {
            // Firestore write may be restricted by security rules for client-side direct writes
            // The localActiveSubscription in-memory & persistent state remains authoritative for the session
        }
    }
}


