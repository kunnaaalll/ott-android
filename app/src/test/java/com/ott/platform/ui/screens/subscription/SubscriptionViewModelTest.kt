package com.ott.platform.ui.screens.subscription

import com.ott.platform.core.payment.PaymentGateway
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.SubscriptionStatus
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.every
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: com.ott.platform.data.repository.UserRepository
    private lateinit var paymentGateway: PaymentGateway
    private lateinit var viewModel: SubscriptionViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        subscriptionRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        paymentGateway = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads plans and subscription state`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        val plan = Plan(id = "plan1")
        val firebaseUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "user1"
        }
        
        coEvery { authRepository.currentUser } returns flowOf(firebaseUser)
        coEvery { userRepository.observeUser("user1") } returns flowOf(user)
        coEvery { subscriptionRepository.getPlans("tenant1") } returns listOf(plan)
        coEvery { subscriptionRepository.observeSubscription("user1", "tenant1") } returns flowOf(null)

        viewModel = SubscriptionViewModel(subscriptionRepository, authRepository, userRepository, paymentGateway)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.plans.value.size)
        assertTrue(viewModel.subscriptionState.value is SubscriptionState.Inactive)
    }

    @Test
    fun `subscription state becomes active when user has active subscription`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        val firebaseUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "user1"
        }
        val sub = Subscription(
            status = SubscriptionStatus.ACTIVE.name, 
            expiryDate = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 86400000))
        )
        
        coEvery { authRepository.currentUser } returns flowOf(firebaseUser)
        coEvery { userRepository.observeUser("user1") } returns flowOf(user)
        coEvery { subscriptionRepository.getPlans("tenant1") } returns emptyList()
        coEvery { subscriptionRepository.observeSubscription("user1", "tenant1") } returns flowOf(sub)

        viewModel = SubscriptionViewModel(subscriptionRepository, authRepository, userRepository, paymentGateway)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.subscriptionState.value is SubscriptionState.Active)
    }

    @Test
    fun `initiatePayment prevents duplicate calls`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        val firebaseUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "user1"
        }
        val plan = Plan(id = "plan1")
        
        coEvery { authRepository.currentUser } returns flowOf(firebaseUser)
        coEvery { userRepository.observeUser("user1") } returns flowOf(user)
        coEvery { subscriptionRepository.getPlans("tenant1") } returns emptyList()
        coEvery { subscriptionRepository.observeSubscription("user1", "tenant1") } returns flowOf(null)

        viewModel = SubscriptionViewModel(subscriptionRepository, authRepository, userRepository, paymentGateway)
        testDispatcher.scheduler.advanceUntilIdle()

        // Call twice
        viewModel.initiatePayment(plan)
        viewModel.initiatePayment(plan)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify payment gateway was only called once
        coVerify(exactly = 1) { paymentGateway.initiatePayment(user, plan) }
    }
}
