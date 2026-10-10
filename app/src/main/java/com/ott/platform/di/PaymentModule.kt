package com.ott.platform.di

import com.ott.platform.core.payment.ExternalPaymentGatewayAdapter
import com.ott.platform.core.payment.PaymentGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    @Binds
    @Singleton
    abstract fun bindPaymentGateway(
        externalPaymentGatewayAdapter: ExternalPaymentGatewayAdapter
    ): PaymentGateway
}
