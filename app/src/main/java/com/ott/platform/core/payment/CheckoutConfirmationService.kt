package com.ott.platform.core.payment

import com.ott.platform.core.api.NotSignedInException
import com.ott.platform.core.api.OrderStatus
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.core.session.SessionManager
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.repository.SubscriptionRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

sealed class CheckoutConfirmation {
    /** The backend reports an active subscription. */
    data class Confirmed(val subscription: Subscription) : CheckoutConfirmation()

    /** Nothing confirmed yet (payment still processing, user came back without paying, or backend unreachable). */
    object Pending : CheckoutConfirmation()

    /** The backend explicitly reports the order as not paid. */
    object Failed : CheckoutConfirmation()

    /** The app credential is no longer valid; the user must sign in again. */
    object SignedOut : CheckoutConfirmation()
}

/**
 * Turns "the user came back from the browser" into a backend-verified answer.
 *
 * Returning to the app (deep link, resume, or the user tapping "check status") is only a
 * navigation event. This service never infers payment from it: it asks the backend for the order
 * state (when an order id is known) and for the user's subscription, and reports an active
 * subscription only when the backend returns one. If the backend cannot be reached the result is
 * [CheckoutConfirmation.Pending]; no local subscription is ever created.
 */
@Singleton
class CheckoutConfirmationService @Inject constructor(
    private val websiteApiClient: WebsiteApiClient,
    private val subscriptionRepository: SubscriptionRepository,
    private val sessionManager: SessionManager
) {

    suspend fun confirm(
        orderId: String?,
        maxAttempts: Int = 4,
        delayMs: Long = 2000
    ): CheckoutConfirmation {
        var sawExplicitFailure = false

        repeat(maxAttempts.coerceAtLeast(1)) { attempt ->
            if (!orderId.isNullOrBlank()) {
                // Lets the backend verify the order with the gateway and record the entitlement.
                val status = websiteApiClient.checkOrderStatus(orderId).getOrNull()
                sawExplicitFailure = status == OrderStatus.FAILED
            }

            val subscriptionResult = websiteApiClient.fetchSubscription(
                userId = sessionManager.getUserId().orEmpty(),
                email = sessionManager.getSessionUser()?.email.orEmpty(),
                mobile = sessionManager.getPhoneNumber().orEmpty()
            )
            val failure = subscriptionResult.exceptionOrNull()
            if (failure is NotSignedInException) return CheckoutConfirmation.Pending

            val subscription = subscriptionResult.getOrNull()
            if (subscription != null && subscription.isActive()) {
                // Cache what the backend told us so the rest of the app reflects it immediately.
                subscriptionRepository.recordActiveSubscription(subscription)
                sessionManager.clearPendingCheckout()
                return CheckoutConfirmation.Confirmed(subscription)
            }

            if (sawExplicitFailure) return CheckoutConfirmation.Failed
            if (attempt < maxAttempts - 1) delay(delayMs)
        }
        return CheckoutConfirmation.Pending
    }
}
