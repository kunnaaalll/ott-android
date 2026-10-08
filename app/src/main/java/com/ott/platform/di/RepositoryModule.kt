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
}
