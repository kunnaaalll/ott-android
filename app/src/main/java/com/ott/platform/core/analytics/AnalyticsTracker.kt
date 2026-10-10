package com.ott.platform.core.analytics

import com.google.firebase.analytics.ktx.analytics
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, Any>? = null)
    fun setUserId(userId: String)
}

@Singleton
class FirebaseAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    private val analytics = com.google.firebase.ktx.Firebase.analytics

    override fun logEvent(name: String, params: Map<String, Any>?) {
        val bundle = android.os.Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                is Double -> bundle.putDouble(key, value)
            }
        }
        analytics.logEvent(name, bundle)
    }

    override fun setUserId(userId: String) {
        analytics.setUserId(userId)
    }
}
