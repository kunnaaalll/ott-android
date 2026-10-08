package com.ott.platform.core.analytics

import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, Any>? = null)
    fun setUserId(userId: String)
}

@Singleton
class FirebaseAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun logEvent(name: String, params: Map<String, Any>?) {
        // Placeholder for Firebase.analytics.logEvent
    }

    override fun setUserId(userId: String) {
        // Placeholder for Firebase.analytics.setUserId
    }
}
