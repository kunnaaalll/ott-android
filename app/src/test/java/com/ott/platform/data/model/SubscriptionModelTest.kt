package com.ott.platform.data.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SubscriptionModelTest {

    @Test
    fun `active subscription with future currentPeriodEnd is active`() {
        val sub = Subscription(
            status = SubscriptionStatus.ACTIVE.name,
            currentPeriodEnd = System.currentTimeMillis() + 86400000L
        )
        assertTrue(sub.isActive())
    }

    @Test
    fun `active subscription with past currentPeriodEnd is not active`() {
        val sub = Subscription(
            status = SubscriptionStatus.ACTIVE.name,
            currentPeriodEnd = System.currentTimeMillis() - 86400000L
        )
        assertFalse(sub.isActive())
    }

    @Test
    fun `cancelled subscription before period end with cancelAtPeriodEnd is active`() {
        val sub = Subscription(
            status = SubscriptionStatus.CANCELLED.name,
            currentPeriodEnd = System.currentTimeMillis() + 86400000L,
            cancelAtPeriodEnd = true
        )
        assertTrue(sub.isActive())
    }

    @Test
    fun `cancelled subscription after period end is not active`() {
        val sub = Subscription(
            status = SubscriptionStatus.CANCELLED.name,
            currentPeriodEnd = System.currentTimeMillis() - 86400000L,
            cancelAtPeriodEnd = true
        )
        assertFalse(sub.isActive())
    }

    @Test
    fun `cancelled subscription without cancelAtPeriodEnd is not active`() {
        val sub = Subscription(
            status = SubscriptionStatus.CANCELLED.name,
            currentPeriodEnd = System.currentTimeMillis() + 86400000L,
            cancelAtPeriodEnd = false
        )
        assertFalse(sub.isActive())
    }

    @Test
    fun `grace period with valid future deadline is active`() {
        val sub = Subscription(
            status = SubscriptionStatus.GRACE_PERIOD.name,
            currentPeriodEnd = System.currentTimeMillis() - 86400000L,
            gracePeriodEnd = System.currentTimeMillis() + 86400000L
        )
        assertTrue(sub.isActive())
    }

    @Test
    fun `grace period past deadline is not active`() {
        val sub = Subscription(
            status = SubscriptionStatus.GRACE_PERIOD.name,
            currentPeriodEnd = System.currentTimeMillis() - 172800000L,
            gracePeriodEnd = System.currentTimeMillis() - 86400000L
        )
        assertFalse(sub.isActive())
    }

    @Test
    fun `grace period with null deadline is not active`() {
        val sub = Subscription(
            status = SubscriptionStatus.GRACE_PERIOD.name,
            currentPeriodEnd = System.currentTimeMillis() - 86400000L,
            gracePeriodEnd = null
        )
        assertFalse(sub.isActive())
    }

    @Test
    fun `paused subscription is not active even with future period`() {
        val sub = Subscription(
            status = SubscriptionStatus.PAUSED.name,
            currentPeriodEnd = System.currentTimeMillis() + 86400000L
        )
        assertFalse(sub.isActive())
    }

    @Test
    fun `refunded subscription is immediately revoked`() {
        val sub = Subscription(
            status = SubscriptionStatus.REFUNDED.name,
            currentPeriodEnd = System.currentTimeMillis() + 86400000L
        )
        assertFalse(sub.isActive())
    }

    @Test
    fun `expired and failed subscriptions are not active`() {
        val expiredSub = Subscription(
            status = SubscriptionStatus.EXPIRED.name,
            currentPeriodEnd = System.currentTimeMillis() + 86400000L
        )
        assertFalse(expiredSub.isActive())

        val failedSub = Subscription(
            status = SubscriptionStatus.FAILED.name,
            currentPeriodEnd = System.currentTimeMillis() + 86400000L
        )
        assertFalse(failedSub.isActive())
    }

    @Test
    fun `legacy expiryDate compatibility works when currentPeriodEnd is omitted`() {
        val futureDate = Date(System.currentTimeMillis() + 86400000L)
        val sub = Subscription(
            status = SubscriptionStatus.ACTIVE.name,
            expiryDate = Timestamp(futureDate)
        )
        assertTrue(sub.isActive())

        val pastDate = Date(System.currentTimeMillis() - 86400000L)
        val expiredLegacy = Subscription(
            status = SubscriptionStatus.ACTIVE.name,
            expiryDate = Timestamp(pastDate)
        )
        assertFalse(expiredLegacy.isActive())
    }

    @Test
    fun `unknown status falls back safely to EXPIRED`() {
        val status = SubscriptionStatus.fromString("BOGUS_STATUS")
        assertEquals(SubscriptionStatus.EXPIRED, status)

        val sub = Subscription(
            status = "BOGUS_STATUS",
            currentPeriodEnd = System.currentTimeMillis() + 86400000L
        )
        assertFalse(sub.isActive())
    }
}
