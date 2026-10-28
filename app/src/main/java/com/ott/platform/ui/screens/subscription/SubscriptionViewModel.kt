package com.ott.platform.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.core.api.CheckoutHandoffException
import com.ott.platform.core.api.NotSignedInException
import com.ott.platform.core.payment.CheckoutConfirmation
import com.ott.platform.core.payment.CheckoutConfirmationService
import com.ott.platform.core.payment.PaymentGateway
import com.ott.platform.core.session.SessionManager
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.SubscriptionStatus
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.SubscriptionRepository
import com.ott.platform.data.repository.UserRepository
import com.ott.platform.core.analytics.MetaAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val userRepository: UserRepository,
    private val paymentGateway: PaymentGateway,
    private val metaAnalyticsTracker: MetaAnalyticsTracker,
    private val checkoutConfirmation: CheckoutConfirmationService? = null,
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Initial)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _isPaymentProcessing = MutableStateFlow(false)
    val isPaymentProcessing: StateFlow<Boolean> = _isPaymentProcessing.asStateFlow()

    /**
     * One-shot "open this website checkout URL in the browser" events. The URL carries a single-use
     * secret in its fragment, so it is delivered once to the UI and never stored or logged.
     */
    private val _checkoutLaunch = Channel<String>(Channel.BUFFERED)
    val checkoutLaunch: Flow<String> = _checkoutLaunch.receiveAsFlow()

    init {
        loadSubscription()
        loadPlans()
    }

    /**
     * Called when the subscription screen returns to the foreground. If the user was just sent to
     * the website checkout, ask the backend what happened. This only reads backend state.
     */
    fun onScreenResumed() {
        if (sessionManager?.hasRecentPendingCheckout() != true) return
        if (_subscriptionState.value is SubscriptionState.Confirming) return
        confirmWithBackend(orderId = null, maxAttempts = 4, delayMs = 1500)
    }

    fun refreshSubscription() {
        loadSubscription()
    }

    private suspend fun resolveEffectiveUser(): User? {
        val sessionUser = sessionManager?.getSessionUser()
        if (sessionUser != null && sessionUser.id.isNotBlank()) return sessionUser

        val uid = authRepository.getCurrentUserId()
        if (!uid.isNullOrBlank()) {
            val user = userRepository.observeUser(uid).firstOrNull()
            if (user != null) return user
        }

        val firebaseUser = authRepository.currentUser.firstOrNull()
        if (firebaseUser != null) {
            val user = userRepository.observeUser(firebaseUser.uid).firstOrNull()
            if (user != null) return user
        }

        return null
    }

    private fun loadSubscription() {
        viewModelScope.launch {
            val effectiveUser = resolveEffectiveUser()

            if (effectiveUser == null) {
                // Anonymous or unauthenticated: present inactive plans cleanly without error banner
                _subscriptionState.value = SubscriptionState.Inactive
                return@launch
            }

            _subscriptionState.value = SubscriptionState.Loading

            subscriptionRepository.observeSubscription(effectiveUser.id, effectiveUser.tenantId).collectLatest { subscription ->
                if (subscription == null) {
                    _subscriptionState.value = SubscriptionState.Inactive
                } else {
                    when (SubscriptionStatus.fromString(subscription.status)) {
                        SubscriptionStatus.ACTIVE,
                        SubscriptionStatus.GRACE_PERIOD -> {
                            if (subscription.isActive()) {
                                _subscriptionState.value = SubscriptionState.Active(subscription)

                                // Emit Meta App Event (fb_mobile_purchase) with deduplication order_id
                                val orderId = subscription.providerSubscriptionId?.takeIf { it.isNotBlank() }
                                    ?: subscription.id.removePrefix("sub_").takeIf { it.isNotBlank() }
                                if (!orderId.isNullOrBlank()) {
                                    val matchedPlan = _plans.value.firstOrNull { it.id == subscription.planId }
                                        ?: com.ott.platform.data.model.PlanDefaults.getPlans(effectiveUser.tenantId).firstOrNull { it.id == subscription.planId }
                                    val planPrice = matchedPlan?.price ?: 0.0
                                    val planName = matchedPlan?.name ?: "Subscription Pass"
                                    metaAnalyticsTracker.logPurchase(
                                        orderId = orderId,
                                        planId = subscription.planId,
                                        planName = planName,
                                        amount = planPrice,
                                        currency = "INR"
                                    )
                                }
                            } else {
                                _subscriptionState.value = SubscriptionState.Expired
                            }
                        }
                        SubscriptionStatus.PENDING -> {
                            _subscriptionState.value = SubscriptionState.Pending
                        }
                        SubscriptionStatus.PAUSED -> {
                            _subscriptionState.value = SubscriptionState.Inactive
                        }
                        SubscriptionStatus.EXPIRED -> {
                            _subscriptionState.value = SubscriptionState.Expired
                        }
                        SubscriptionStatus.REFUNDED -> {
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
                val user = resolveEffectiveUser()
                val tenantId = user?.tenantId ?: "tenant_chumbak"
                val loadedPlans = subscriptionRepository.getPlans(tenantId)
                val rawPlans = if (loadedPlans.isNotEmpty()) loadedPlans else com.ott.platform.data.model.PlanDefaults.getPlans(tenantId)
                _plans.value = rawPlans.filter { it.price > 1.0 }.sortedBy { it.price }
            } catch (e: Exception) {
                _plans.value = com.ott.platform.data.model.PlanDefaults.getPlans("tenant_chumbak").filter { it.price > 1.0 }.sortedBy { it.price }
            }
        }
    }

    fun getSavedPhone(): String? = sessionManager?.getPhoneNumber()

    fun savePhoneNumber(phone: String) {
        sessionManager?.savePhoneNumber(phone)
    }

    fun isPhoneRequired(): Boolean {
        val user = sessionManager?.getSessionUser()
        val emailDigits = user?.email?.filter { it.isDigit() } ?: ""
        if (emailDigits.length >= 10) return false
        val saved = sessionManager?.getPhoneNumber()
        return saved.isNullOrBlank()
    }

    /**
     * Starts the web checkout. The backend validates the plan and price and issues a single-use
     * handoff; the browser then opens the website checkout already signed in. Nothing here
     * activates a subscription.
     */
    fun initiatePayment(plan: Plan) {
        if (_isPaymentProcessing.value) return
        _isPaymentProcessing.value = true

        viewModelScope.launch {
            try {
                resolveEffectiveUser() ?: throw IllegalStateException("Please log in to purchase a subscription.")
                val handoff = paymentGateway.startWebCheckout(plan)
                sessionManager?.savePendingCheckout(plan.id)
                _checkoutLaunch.send(handoff.url)
            } catch (e: NotSignedInException) {
                _subscriptionState.value = SubscriptionState.Error("Your session has expired. Please sign in again to continue.")
                try {
                    authRepository.signOut()
                } catch (signOutError: Exception) {
                    // Navigation guard handles the rest; nothing else to do.
                }
            } catch (e: CheckoutHandoffException) {
                _subscriptionState.value = SubscriptionState.Error(e.message ?: "Could not start checkout. Please try again.")
            } catch (e: IllegalStateException) {
                _subscriptionState.value = SubscriptionState.Error(e.message ?: "Payment initiation failed")
            } catch (e: Exception) {
                _subscriptionState.value = SubscriptionState.Error(
                    e.message?.takeIf { !it.contains("Exception") && it.isNotBlank() }
                        ?: "Couldn't start checkout. Check your connection and try again."
                )
            } finally {
                _isPaymentProcessing.value = false
            }
        }
    }

    /**
     * The user came back from the browser (deep link, e.g. chumbak://subscription/status?order_id=...).
     * That is a navigation event, not proof of payment: the outcome comes from the backend.
     */
    fun handleCheckoutReturn(orderId: String? = null) {
        confirmWithBackend(orderId, maxAttempts = 6, delayMs = 2000)
    }

    /** "Check payment status" button. */
    fun retryCheckPayment() {
        confirmWithBackend(orderId = null, maxAttempts = 4, delayMs = 1500)
    }

    private fun confirmWithBackend(orderId: String?, maxAttempts: Int, delayMs: Long) {
        val service = checkoutConfirmation ?: return
        _subscriptionState.value = SubscriptionState.Confirming

        viewModelScope.launch {
            when (val outcome = service.confirm(orderId, maxAttempts, delayMs)) {
                is CheckoutConfirmation.Confirmed -> {
                    _subscriptionState.value = SubscriptionState.Active(outcome.subscription)
                    val planId = outcome.subscription.planId
                    val plan = _plans.value.firstOrNull { it.id == planId }
                    val orderRef = outcome.subscription.providerSubscriptionId?.takeIf { it.isNotBlank() }
                        ?: outcome.subscription.id.removePrefix("sub_")
                    if (plan != null && orderRef.isNotBlank()) {
                        // Meta purchase event is deduplicated by order id.
                        metaAnalyticsTracker.logPurchase(
                            orderId = orderRef,
                            planId = plan.id,
                            planName = plan.name,
                            amount = plan.price,
                            currency = "INR"
                        )
                    }
                }
                CheckoutConfirmation.Pending -> {
                    _subscriptionState.value = SubscriptionState.ConfirmingTimeout
                }
                CheckoutConfirmation.Failed -> {
                    sessionManager?.clearPendingCheckout()
                    _subscriptionState.value = SubscriptionState.Error("Payment was not completed. You can try again.")
                }
                CheckoutConfirmation.SignedOut -> {
                    _subscriptionState.value = SubscriptionState.Inactive
                }
            }
        }
    }
}
