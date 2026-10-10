package com.ott.platform.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.PlanRepository
import com.ott.platform.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminPlanViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _planList = MutableStateFlow<List<Plan>>(emptyList())
    val planList: StateFlow<List<Plan>> = _planList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentUser: User? = null

    init {
        loadAdminData()
    }

    private fun loadAdminData() {
        viewModelScope.launch {
            _isLoading.value = true
            val fbUser = authRepository.currentUser.firstOrNull()
            if (fbUser != null) {
                val user = userRepository.observeUser(fbUser.uid).firstOrNull()
                if (user != null && user.isAdmin) {
                    currentUser = user
                    observePlans(user.tenantId)
                } else {
                    _errorMessage.value = "Unauthorized"
                    _isLoading.value = false
                }
            } else {
                _errorMessage.value = "Unauthenticated"
                _isLoading.value = false
            }
        }
    }

    private fun observePlans(tenantId: String) {
        viewModelScope.launch {
            try {
                planRepository.observeAllPlans(tenantId).collectLatest {
                    _planList.value = it
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load plans: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    fun savePlan(plan: Plan) {
        val user = currentUser ?: return
        
        if (plan.tenantId.isNotEmpty() && plan.tenantId != user.tenantId) {
            _errorMessage.value = "Permission denied"
            return
        }

        val securedPlan = plan.copy(tenantId = user.tenantId)

        if (securedPlan.name.isBlank()) {
            _errorMessage.value = "Name is required"
            return
        }
        if (securedPlan.price < 0) {
            _errorMessage.value = "Price must be positive"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                planRepository.savePlan(securedPlan)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save plan: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deactivatePlan(planId: String) {
        viewModelScope.launch {
            try {
                planRepository.deactivatePlan(planId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to deactivate: ${e.localizedMessage}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
