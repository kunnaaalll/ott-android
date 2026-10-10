package com.ott.platform.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.ott.platform.data.model.Plan
import com.ott.platform.data.repository.PlanRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PlanRepository {

    private val plansCollection = firestore.collection("plans")

    override fun observeActivePlans(tenantId: String): Flow<List<Plan>> = callbackFlow {
        val listener = plansCollection
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Plan::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun observeAllPlans(tenantId: String): Flow<List<Plan>> = callbackFlow {
        val listener = plansCollection
            .whereEqualTo("tenantId", tenantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Plan::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPlanById(planId: String): Plan? {
        val snapshot = plansCollection.document(planId).get().await()
        return snapshot.toObject(Plan::class.java)
    }

    override suspend fun savePlan(plan: Plan) {
        val docRef = if (plan.id.isEmpty()) plansCollection.document() else plansCollection.document(plan.id)
        docRef.set(plan).await()
    }

    override suspend fun deactivatePlan(planId: String) {
        plansCollection.document(planId).update("active", false).await()
    }
}
