package com.ott.platform.core.payment

import com.ott.platform.core.api.NotSignedInException
import com.ott.platform.core.api.OrderStatus
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.core.session.SessionManager
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.SubscriptionStatus
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Returning from the browser is a navigation event. These tests pin down that the app only
 * reports an active subscription when the backend returns one, and never creates one itself.
 */
class CheckoutConfirmationServiceTest {

    private lateinit var api: WebsiteApiClient
    private lateinit var subscriptions: SubscriptionRepository
    private lateinit var sessions: SessionManager
    private lateinit var service: CheckoutConfirmationService

    private val active = Subscription(
        id = "sub_order_1",
        userId = "user1",
        planId = "plan_one_month",
        status = SubscriptionStatus.ACTIVE.name,
        currentPeriodEnd = System.currentTimeMillis() + 86_400_000L
    )

    @Before
    fun setUp() {
        api = mockk()
        subscriptions = mockk(relaxed = true)
        sessions = mockk(relaxed = true) {
            every { getUserId() } returns "user1"
            every { getSessionUser() } returns User(id = "user1", email = "a@b.test")
            every { getPhoneNumber() } returns null
        }
        service = CheckoutConfirmationService(api, subscriptions, sessions)
    }

    @Test
    fun `confirms only when the backend returns an active subscription and caches that server answer`() = runTest {
        coEvery { api.checkOrderStatus("order_1") } returns Result.success(OrderStatus.PAID)
        coEvery { api.fetchSubscription(any(), any(), any()) } returns Result.success(active)

        val outcome = service.confirm("order_1", maxAttempts = 3, delayMs = 0)

        assertEquals(CheckoutConfirmation.Confirmed(active), outcome)
        coVerify { subscriptions.recordActiveSubscription(active) }
        verify { sessions.clearPendingCheckout() }
    }

    @Test
    fun `order reported paid but entitlement not visible yet stays pending, never active`() = runTest {
        coEvery { api.checkOrderStatus(any()) } returns Result.success(OrderStatus.PAID)
        coEvery { api.fetchSubscription(any(), any(), any()) } returns Result.success(null)

        val outcome = service.confirm("order_1", maxAttempts = 3, delayMs = 0)

        assertEquals(CheckoutConfirmation.Pending, outcome)
        coVerify(exactly = 0) { subscriptions.recordActiveSubscription(any()) }
        coVerify(exactly = 3) { api.fetchSubscription(any(), any(), any()) } // safe retries, read-only
    }

    @Test
    fun `an expired subscription from the backend is not treated as active`() = runTest {
        val expired = active.copy(currentPeriodEnd = System.currentTimeMillis() - 1000L)
        coEvery { api.fetchSubscription(any(), any(), any()) } returns Result.success(expired)

        val outcome = service.confirm(null, maxAttempts = 2, delayMs = 0)

        assertEquals(CheckoutConfirmation.Pending, outcome)
        coVerify(exactly = 0) { subscriptions.recordActiveSubscription(any()) }
    }

    @Test
    fun `backend unavailable yields pending and creates no local subscription`() = runTest {
        coEvery { api.checkOrderStatus(any()) } returns Result.failure(IOException("offline"))
        coEvery { api.fetchSubscription(any(), any(), any()) } returns Result.failure(IOException("offline"))

        val outcome = service.confirm("order_1", maxAttempts = 3, delayMs = 0)

        assertEquals(CheckoutConfirmation.Pending, outcome)
        coVerify(exactly = 0) { subscriptions.recordActiveSubscription(any()) }
    }

    @Test
    fun `a failed or cancelled order is reported as failed without retrying`() = runTest {
        coEvery { api.checkOrderStatus("order_1") } returns Result.success(OrderStatus.FAILED)
        coEvery { api.fetchSubscription(any(), any(), any()) } returns Result.success(null)

        val outcome = service.confirm("order_1", maxAttempts = 5, delayMs = 0)

        assertEquals(CheckoutConfirmation.Failed, outcome)
        coVerify(exactly = 1) { api.fetchSubscription(any(), any(), any()) }
        coVerify(exactly = 0) { subscriptions.recordActiveSubscription(any()) }
    }

    @Test
    fun `a pending order keeps polling until attempts run out`() = runTest {
        coEvery { api.checkOrderStatus("order_1") } returns Result.success(OrderStatus.PENDING)
        coEvery { api.fetchSubscription(any(), any(), any()) } returns Result.success(null)

        val outcome = service.confirm("order_1", maxAttempts = 4, delayMs = 0)

        assertEquals(CheckoutConfirmation.Pending, outcome)
        coVerify(exactly = 4) { api.checkOrderStatus("order_1") }
    }

    @Test
    fun `without an order id only the subscription is read`() = runTest {
        coEvery { api.fetchSubscription(any(), any(), any()) } returns Result.success(active)

        service.confirm(null, maxAttempts = 1, delayMs = 0)

        coVerify(exactly = 0) { api.checkOrderStatus(any()) }
    }

    @Test
    fun `an invalid app session is reported so the user can sign in again`() = runTest {
        coEvery { api.checkOrderStatus(any()) } returns Result.success(OrderStatus.PENDING)
        coEvery { api.fetchSubscription(any(), any(), any()) } returns Result.failure(NotSignedInException())

        val outcome = service.confirm("order_1", maxAttempts = 3, delayMs = 0)

        assertTrue(outcome is CheckoutConfirmation.SignedOut)
        coVerify(exactly = 0) { subscriptions.recordActiveSubscription(any()) }
    }
}
