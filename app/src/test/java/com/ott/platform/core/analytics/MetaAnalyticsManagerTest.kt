package com.ott.platform.core.analytics

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MetaAnalyticsManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val savedOrders = mutableSetOf<String>()

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        savedOrders.clear()
        every { mockContext.getSharedPreferences(any(), Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.getStringSet("logged_purchase_order_ids", any()) } answers { savedOrders }
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putStringSet(any(), any()) } answers {
            val set = secondArg<Set<String>>()
            savedOrders.clear()
            savedOrders.addAll(set)
            mockEditor
        }
        every { mockEditor.apply() } returns Unit
    }

    @Test
    fun `isPurchaseAlreadyLogged returns false for unlogged orders`() {
        val manager = MetaAnalyticsManager(mockContext)
        assertFalse(manager.isPurchaseAlreadyLogged("order_unseen_123"))
    }

    @Test
    fun `isPurchaseAlreadyLogged returns true for previously persisted orders`() {
        savedOrders.add("order_preexisting_999")
        val manager = MetaAnalyticsManager(mockContext)
        assertTrue(manager.isPurchaseAlreadyLogged("order_preexisting_999"))
    }

    @Test
    fun `logPurchase rejects blank or invalid orderId`() {
        val manager = MetaAnalyticsManager(mockContext)
        assertFalse(manager.logPurchase(
            orderId = "",
            planId = "plan_1",
            planName = "Pass",
            amount = 199.0
        ))
        assertFalse(manager.logPurchase(
            orderId = "   ",
            planId = "plan_1",
            planName = "Pass",
            amount = 199.0
        ))
    }

    @Test
    fun `isPurchaseAlreadyLogged handles blank orderId safely`() {
        val manager = MetaAnalyticsManager(mockContext)
        assertFalse(manager.isPurchaseAlreadyLogged(""))
        assertFalse(manager.isPurchaseAlreadyLogged("   "))
    }
}
