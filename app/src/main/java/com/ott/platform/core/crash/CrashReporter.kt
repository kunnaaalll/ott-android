package com.ott.platform.core.crash

import com.google.firebase.crashlytics.ktx.crashlytics
import javax.inject.Inject
import javax.inject.Singleton

interface CrashReporter {
    fun logException(throwable: Throwable)
    fun setUserId(userId: String)
    fun setCustomKey(key: String, value: String)
}

@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {
    private val crashlytics = com.google.firebase.ktx.Firebase.crashlytics

    override fun logException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
