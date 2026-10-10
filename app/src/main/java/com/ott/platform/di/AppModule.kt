package com.ott.platform.di

import com.ott.platform.core.analytics.AnalyticsTracker
import com.ott.platform.core.analytics.FirebaseAnalyticsTracker
import com.ott.platform.core.crash.CrashReporter
import com.ott.platform.core.crash.FirebaseCrashReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindAnalyticsTracker(
        impl: FirebaseAnalyticsTracker
    ): AnalyticsTracker

    @Binds
    abstract fun bindCrashReporter(
        impl: FirebaseCrashReporter
    ): CrashReporter
}
