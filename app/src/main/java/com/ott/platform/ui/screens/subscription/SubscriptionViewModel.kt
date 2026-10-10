package com.ott.platform.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.core.payment.PaymentGateway
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.SubscriptionStatus
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SubscriptionState {
    object Initial : SubscriptionState()
    object Loading : SubscriptionState()
    data class Active(val subscription: Subscription) : SubscriptionState()
    object Inactive : SubscriptionState()
    object Pending : SubscriptionState()
    object Expired : SubscriptionState()
    object Cancelled : SubscriptionState()
    object Confirming : SubscriptionState()
    object ConfirmingTimeout : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository,
    private val paymentGateway: PaymentGateway
) : ViewModel() {

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Initial)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _isPaymentProcessing = MutableStateFlow(false)
    val isPaymentProcessing: StateFlow<Boolean> = _isPaymentProcessing.asStateFlow()

    init {
        loadSubscription()
        loadPlans()
    }

    private fun loadSubscription() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _subscriptionState.value = SubscriptionState.Error("User not authenticated")
                return@launch
            }

            _subscriptionState.value = SubscriptionState.Loading

            subscriptionRepository.observeSubscription(user.id, user.tenantId).collectLatest { subscription ->
                if (subscription == null) {
                    _subscriptionState.value = SubscriptionState.Inactive
                } else {
                    when (SubscriptionStatus.fromString(subscription.status)) {
                        SubscriptionStatus.ACTIVE -> {
                            if (subscription.isActive()) {
                                _subscriptionState.value = SubscriptionState.Active(subscription)
                            } else {
                                _subscriptionState.value = SubscriptionState.Expired
                            }
                        }
                        SubscriptionStatus.PENDING -> {
                            _subscriptionState.value = SubscriptionState.Pending
                        }
                        SubscriptionStatus.EXPIRED -> {
                            _subscriptionState.value = SubscriptionState.Expired
                        }
                        SubscriptionStatus.CANCELLED -> {
                            if (subscription.isActive()) {
                                _subscriptionState.value = SubscriptionState.Active(subscription)
                            } else {
                                _subscriptionState.value = SubscriptionState.Cancelled
                            }
                        }
                        SubscriptionStatus.FAILED -> {
                            _subscriptionState.value = SubscriptionState.Error("Previous payment failed. Please try again.")
                        }
                    }
                }
            }
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser() ?: return@launch
                _plans.value = subscriptionRepository.getPlans(user.tenantId)
            } catch (e: Exception) {
                // Ignore or handle
            }
        }
    }

    fun initiatePayment(plan: Plan) {
        if (_isPaymentProcessing.value) return
        
        viewModelScope.launch {
            _isPaymentProcessing.value = true
            try {
                val user = authRepository.getCurrentUser() ?: throw IllegalStateException("Not logged in")
                paymentGateway.initiatePayment(user, plan)
                
                // If payment returns successfully (e.g. user completed UI flow),
                // wait for the Firestore webhook to update the status to ACTIVE.
                _subscriptionState.value = SubscriptionState.Confirming
                
                launch {
                    kotlinx.coroutines.delay(15000)
                    if (_subscriptionState.value is SubscriptionState.Confirming) {
                        _subscriptionState.value = SubscriptionState.ConfirmingTimeout
                    }
                }
            } catch (e: Exception) {
                _subscriptionState.value = SubscriptionState.Error(e.message ?: "Payment initiation failed")
            } finally {
                _isPaymentProcessing.value = false
            }
        }
    }
}
