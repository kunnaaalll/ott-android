package com.ott.platform

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.ott.platform.core.analytics.MetaAnalyticsTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main application class.
 * Triggers Hilt code generation for dependency injection.
 */
@HiltAndroidApp
class OttApplication : Application() {

    @Inject
    lateinit var metaAnalyticsTracker: MetaAnalyticsTracker

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:abcdef123456")
                .setApiKey("AIzaSyExampleKeyNotRealLocalMock")
                .setProjectId("ott-project")
                .build()
            FirebaseApp.initializeApp(this, options)
        }

        // Initialize Meta App Events SDK with privacy controls
        metaAnalyticsTracker.initialize()
    }
}
