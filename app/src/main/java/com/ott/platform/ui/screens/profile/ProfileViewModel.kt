package com.ott.platform.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.SubscriptionRepository
import com.ott.platform.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val tenantId: String = "",
    val isAdmin: Boolean = false,
    val subscriptionStatus: String = "Inactive",
    val planName: String = "None",
    val expiryDate: String? = null,
    val hasActiveSubscription: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepository.getCurrentUserId()
        if (uid.isNullOrBlank()) {
            _uiState.value = ProfileUiState(isLoading = false)
            return
        }

        viewModelScope.launch {
            userRepository.observeUser(uid)
                .filterNotNull()
                .collectLatest { user ->
                    val tenantId = user.tenantId.ifBlank { "tenant-default" }
                    
                    // Observe subscription for this user
                    subscriptionRepository.observeSubscription(uid, tenantId)
                        .collectLatest { subscription ->
                            val plans = try {
                                subscriptionRepository.getPlans(tenantId)
                            } catch (e: Exception) {
                                emptyList()
                            }
                            
                            val planName = plans.find { it.id == subscription?.planId }?.name 
                                ?: subscription?.planId?.takeIf { it.isNotBlank() } 
                                ?: if (subscription?.isActive() == true) "Active Plan" else "None"

                            val expiryFormatted = formatExpiry(subscription)

                            _uiState.value = ProfileUiState(
                                name = user.name.ifBlank { "Member" },
                                email = user.email,
                                tenantId = user.tenantId,
                                isAdmin = user.isAdmin,
                                subscriptionStatus = if (subscription?.isActive() == true) "Active" else "Inactive",
                                planName = planName,
                                expiryDate = expiryFormatted,
                                hasActiveSubscription = subscription?.isActive() == true,
                                isLoading = false
                            )
                        }
                }
        }
    }

    private fun formatExpiry(subscription: Subscription?): String? {
        if (subscription == null) return null
        val timeMs = subscription.currentPeriodEnd 
            ?: subscription.expiryDate?.toDate()?.time
            ?: return null

        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(Date(timeMs))
    }
}
