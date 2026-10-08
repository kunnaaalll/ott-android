package com.ott.platform

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class.
 * Triggers Hilt code generation for dependency injection.
 */
@HiltAndroidApp
class OttApplication : Application()
