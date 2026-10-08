package com.ott.platform.core.crash

import javax.inject.Inject
import javax.inject.Singleton

interface CrashReporter {
    fun logException(throwable: Throwable)
    fun setUserId(userId: String)
    fun setCustomKey(key: String, value: String)
}

@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {
    override fun logException(throwable: Throwable) {
        // Placeholder for Firebase.crashlytics.recordException
    }

    override fun setUserId(userId: String) {
        // Placeholder for Firebase.crashlytics.setUserId
    }

    override fun setCustomKey(key: String, value: String) {
        // Placeholder for Firebase.crashlytics.setCustomKey
    }
}
