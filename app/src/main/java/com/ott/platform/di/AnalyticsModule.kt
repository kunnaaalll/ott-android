package com.ott.platform.di

import com.ott.platform.core.analytics.MetaAnalyticsManager
import com.ott.platform.core.analytics.MetaAnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindMetaAnalyticsTracker(
        metaAnalyticsManager: MetaAnalyticsManager
    ): MetaAnalyticsTracker
}
