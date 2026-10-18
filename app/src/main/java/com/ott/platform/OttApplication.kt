package com.ott.platform

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class.
 * Triggers Hilt code generation for dependency injection.
 */
@HiltAndroidApp
class OttApplication : Application() {
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
    }
}
