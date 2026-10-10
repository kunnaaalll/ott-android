package com.ott.platform.data.repository

import com.ott.platform.data.model.Plan
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun observeActivePlans(tenantId: String): Flow<List<Plan>>
    fun observeAllPlans(tenantId: String): Flow<List<Plan>>
    suspend fun getPlanById(planId: String): Plan?
    suspend fun savePlan(plan: Plan)
    suspend fun deactivatePlan(planId: String)
}
