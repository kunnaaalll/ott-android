package com.ott.platform.core.analytics

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.util.Collections
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-grade Meta (Facebook) App Events tracker.
 *
 * Implements:
 * 1. Safe, lifecycle-aware initialization with privacy and tracking consent guards.
 * 2. Disabled automatic in-app billing collection (prevents incorrect Google Play interception for web-checkout).
 * 3. Client-side idempotency cache to prevent duplicate purchase emissions from activity recreation/deep link callbacks.
 * 4. Exact `event_id` propagation for reliable deduplication against Meta Conversions API (CAPI).
 * 5. Isolation between tracking and entitlement granting.
 */
@Singleton
class MetaAnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : MetaAnalyticsTracker {

    companion object {
        private const val TAG = "MetaAnalytics"
        private const val PREFS_NAME = "ott_meta_analytics_prefs"
        private const val KEY_LOGGED_ORDERS = "logged_purchase_order_ids"
        private const val MAX_CACHED_ORDERS = 500
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // In-memory synchronized set initialized from persistent storage
    private val loggedOrderIds: MutableSet<String> = Collections.synchronizedSet(HashSet())

    private var logger: AppEventsLogger? = null
    private var isInitialized = false

    init {
        // Load persisted orders into memory cache
        val savedOrders = prefs.getStringSet(KEY_LOGGED_ORDERS, emptySet()) ?: emptySet()
        loggedOrderIds.addAll(savedOrders)
    }

    override fun initialize() {
        if (isInitialized) return

        try {
            if (!FacebookSdk.isInitialized()) {
                FacebookSdk.sdkInitialize(context)
            }

            // Disable automatic in-app purchase logging to prevent duplicate/false purchases
            // (Our OTT uses Cashfree secure hosted web checkout, not Google Play In-App Billing)
            FacebookSdk.setAutoLogAppEventsEnabled(false)

            // Respect advertising ID collection default based on privacy requirements
            FacebookSdk.setAdvertiserIDCollectionEnabled(true)

            logger = AppEventsLogger.newLogger(context)
            isInitialized = true
            Log.d(TAG, "Meta App Events SDK initialized successfully.")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize Meta SDK: ${t.message}")
        }
    }

    override fun setTrackingConsent(trackingAllowed: Boolean) {
        try {
            FacebookSdk.setAdvertiserIDCollectionEnabled(trackingAllowed)
            if (!trackingAllowed) {
                // Enable Limited Data Use (LDU) compliance mode when consent is restricted
                FacebookSdk.setDataProcessingOptions(arrayOf("LDU"), 0, 0)
            } else {
                FacebookSdk.setDataProcessingOptions(emptyArray())
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to update tracking consent: ${t.message}")
        }
    }

    override fun logInitiateCheckout(
        planId: String,
        planName: String,
        amount: Double,
        currency: String,
        checkoutSessionId: String?
    ) {
        try {
            ensureLogger()
            val safeAmount = if (amount > 0) amount else 0.0
            val bundle = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "subscription")
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, planId)
                putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency)
                putString("plan_name", planName)
                if (!checkoutSessionId.isNullOrBlank()) {
                    putString("session_id", checkoutSessionId)
                }
            }

            logger?.logEvent(AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT, safeAmount, bundle)
            Log.d(TAG, "Logged InitiateCheckout for plan=$planId, amount=$safeAmount $currency")
        } catch (t: Throwable) {
            Log.w(TAG, "Error logging InitiateCheckout event: ${t.message}")
        }
    }

    override fun logPurchase(
        orderId: String,
        planId: String,
        planName: String,
        amount: Double,
        currency: String,
        extraParams: Bundle?
    ): Boolean {
        val cleanOrderId = orderId.trim()
        if (cleanOrderId.isBlank()) {
            Log.w(TAG, "Refusing to log purchase: empty or invalid orderId.")
            return false
        }

        // Idempotency Gate: Prevent duplicate client purchase tracking
        if (isPurchaseAlreadyLogged(cleanOrderId)) {
            Log.i(TAG, "Purchase event for orderId=$cleanOrderId already logged. Skipping duplicate emission.")
            return false
        }

        try {
            ensureLogger()
            val safeAmount = if (amount > 0) amount else 0.0
            val targetCurrency = try {
                Currency.getInstance(currency.uppercase())
            } catch (_: Throwable) {
                Currency.getInstance("INR")
            }

            val bundle = (extraParams ?: Bundle()).apply {
                // Critical deduplication key matching Conversions API (CAPI) event_id
                putString("event_id", cleanOrderId)
                putString("order_id", cleanOrderId)
                putString(AppEventsConstants.EVENT_PARAM_CURRENCY, targetCurrency.currencyCode)
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "subscription")
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, planId)
                putString("plan_name", planName)
            }

            // Standard Meta Mobile Purchase Event (fb_mobile_purchase)
            logger?.logPurchase(
                BigDecimal.valueOf(safeAmount),
                targetCurrency,
                bundle
            )

            // Record in idempotency cache
            persistLoggedOrder(cleanOrderId)
            Log.i(TAG, "Logged fb_mobile_purchase for orderId=$cleanOrderId, amount=$safeAmount ${targetCurrency.currencyCode}")
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to log purchase event for orderId=$cleanOrderId: ${t.message}")
            return false
        }
    }

    override fun isPurchaseAlreadyLogged(orderId: String): Boolean {
        if (orderId.isBlank()) return false
        return loggedOrderIds.contains(orderId.trim())
    }

    private fun persistLoggedOrder(orderId: String) {
        loggedOrderIds.add(orderId)
        try {
            val current = prefs.getStringSet(KEY_LOGGED_ORDERS, emptySet())?.toMutableSet() ?: mutableSetOf()
            // Bound cache size to prevent unbounded growth over years of app use
            if (current.size >= MAX_CACHED_ORDERS) {
                val toRemove = current.take(50)
                current.removeAll(toRemove.toSet())
            }
            current.add(orderId)
            prefs.edit().putStringSet(KEY_LOGGED_ORDERS, current).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist logged orderId to SharedPreferences: ${e.message}")
        }
    }

    private fun ensureLogger() {
        if (logger == null) {
            initialize()
        }
    }
}
