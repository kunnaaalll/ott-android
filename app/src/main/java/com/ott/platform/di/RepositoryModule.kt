package com.ott.platform.di

import com.ott.platform.data.repository.ContentRepository
import com.ott.platform.data.repository.SubscriptionRepository
import com.ott.platform.data.repository.UserRepository
import com.ott.platform.data.repository.impl.ContentRepositoryImpl
import com.ott.platform.data.repository.impl.SubscriptionRepositoryImpl
import com.ott.platform.data.repository.impl.UserRepositoryImpl
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.impl.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindContentRepository(
        impl: ContentRepositoryImpl
    ): ContentRepository

    @Binds
    abstract fun bindSubscriptionRepository(
        impl: SubscriptionRepositoryImpl
    ): SubscriptionRepository

    @Binds
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindPlaybackAuthorizationRepository(
        impl: com.ott.platform.data.repository.impl.PlaybackAuthorizationRepositoryImpl
    ): com.ott.platform.data.repository.PlaybackAuthorizationRepository

    @Binds
    abstract fun bindMyListRepository(
        impl: com.ott.platform.data.repository.impl.MyListRepositoryImpl
    ): com.ott.platform.data.repository.MyListRepository

    @Binds
    abstract fun bindCategoryRepository(
        impl: com.ott.platform.data.repository.impl.CategoryRepositoryImpl
    ): com.ott.platform.data.repository.CategoryRepository

    @Binds
    abstract fun bindPlanRepository(
        impl: com.ott.platform.data.repository.impl.PlanRepositoryImpl
    ): com.ott.platform.data.repository.PlanRepository

    @Binds
    abstract fun bindMediaUploadRepository(
        impl: com.ott.platform.data.repository.impl.MediaUploadRepositoryImpl
    ): com.ott.platform.data.repository.MediaUploadRepository
}
