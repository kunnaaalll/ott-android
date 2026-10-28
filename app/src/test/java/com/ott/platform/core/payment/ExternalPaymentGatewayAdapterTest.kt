package com.ott.platform.core.payment

import com.ott.platform.core.analytics.MetaAnalyticsTracker
import com.ott.platform.core.api.CheckoutHandoff
import com.ott.platform.core.api.CheckoutHandoffException
import com.ott.platform.core.api.NotSignedInException
import com.ott.platform.core.api.OrderStatus
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.SubscriptionStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ExternalPaymentGatewayAdapterTest {

    private lateinit var api: WebsiteApiClient
    private lateinit var analytics: MetaAnalyticsTracker
    private lateinit var adapter: ExternalPaymentGatewayAdapter

    private val plan = Plan(id = "plan_one_month", name = "1 Month", price = 279.0)
    private val handoff = CheckoutHandoff(
        url = "https://thechumbak.com/checkout/handoff#t=" + "B".repeat(43),
        planId = "plan_one_month",
        expiresAtMs = 1L
    )

    @Before
    fun setUp() {
        api = mockk()
        analytics = mockk(relaxed = true)
        adapter = ExternalPaymentGatewayAdapter(api, analytics)
    }

    @Test
    fun `asks the backend for a handoff by plan id only and returns it untouched`() = runTest {
        coEvery { api.requestCheckoutHandoff("plan_one_month") } returns Result.success(handoff)

        assertEquals(handoff, adapter.startWebCheckout(plan))

        coVerify(exactly = 1) { api.requestCheckoutHandoff("plan_one_month") }
        // No order, price or payment session is created or sent by the app.
        coVerify(exactly = 0) { api.checkOrderStatus(any()) }
    }

    @Test
    fun `analytics never receives the handoff url or token`() = runTest {
        coEvery { api.requestCheckoutHandoff(any()) } returns Result.success(handoff)

        adapter.startWebCheckout(plan)

        verify {
            analytics.logInitiateCheckout(
                planId = "plan_one_month",
                planName = "1 Month",
                amount = 279.0,
                currency = "INR",
                checkoutSessionId = null
            )
        }
    }

    @Test
    fun `propagates a missing or expired app session`() = runTest {
        coEvery { api.requestCheckoutHandoff(any()) } returns Result.failure(NotSignedInException())
        assertThrows(NotSignedInException::class.java) { kotlinx.coroutines.runBlocking { adapter.startWebCheckout(plan) } }
    }

    @Test
    fun `propagates backend rejections such as an invalid plan`() = runTest {
        coEvery { api.requestCheckoutHandoff(any()) } returns
            Result.failure(CheckoutHandoffException("INVALID_PLAN", "That subscription plan is not available."))
        val error = assertThrows(CheckoutHandoffException::class.java) { kotlinx.coroutines.runBlocking { adapter.startWebCheckout(plan) } }
        assertEquals("INVALID_PLAN", error.code)
        verify(exactly = 0) { analytics.logInitiateCheckout(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `order status is ACTIVE only when the backend says PAID`() = runTest {
        coEvery { api.checkOrderStatus("paid") } returns Result.success(OrderStatus.PAID)
        coEvery { api.checkOrderStatus("pending") } returns Result.success(OrderStatus.PENDING)
        coEvery { api.checkOrderStatus("failed") } returns Result.success(OrderStatus.FAILED)
        coEvery { api.checkOrderStatus("down") } returns Result.failure(java.io.IOException("offline"))

        assertEquals(SubscriptionStatus.ACTIVE.name, adapter.getSubscriptionStatus("paid"))
        assertEquals(SubscriptionStatus.PENDING.name, adapter.getSubscriptionStatus("pending"))
        assertEquals(SubscriptionStatus.PENDING.name, adapter.getSubscriptionStatus("failed"))
        assertEquals(SubscriptionStatus.PENDING.name, adapter.getSubscriptionStatus("down"))
    }
}
