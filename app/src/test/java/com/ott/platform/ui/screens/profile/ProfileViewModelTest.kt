package com.ott.platform.ui.screens.profile

import app.cash.turbine.test
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.SubscriptionStatus
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.SubscriptionRepository
import com.ott.platform.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        userRepository = mockk()
        subscriptionRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when unauthenticated uiState sets isLoading false with empty data`() = runTest {
        every { authRepository.getCurrentUserId() } returns null

        val viewModel = ProfileViewModel(authRepository, userRepository, subscriptionRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("", state.email)
            assertEquals("", state.name)
            assertFalse(state.hasActiveSubscription)
        }
    }

    @Test
    fun `when authenticated uiState populates user and active subscription`() = runTest {
        val testUid = "user-123"
        val testTenant = "tenant-chumbak"
        val testUser = User(
            id = testUid,
            tenantId = testTenant,
            name = "Test User",
            email = "user@thechumbak.com",
            isAdmin = true
        )
        val testSubscription = Subscription(
            id = "sub-123",
            tenantId = testTenant,
            userId = testUid,
            planId = "plan-premium",
            status = SubscriptionStatus.ACTIVE.name,
            currentPeriodEnd = System.currentTimeMillis() + 86400000L * 30
        )
        val testPlan = Plan(
            id = "plan-premium",
            name = "All Access VIP",
            price = 499.0,
            billingPeriod = "monthly"
        )

        every { authRepository.getCurrentUserId() } returns testUid
        every { userRepository.observeUser(testUid) } returns flowOf(testUser)
        every { subscriptionRepository.observeSubscription(testUid, testTenant) } returns flowOf(testSubscription)
        coEvery { subscriptionRepository.getPlans(testTenant) } returns listOf(testPlan)

        val viewModel = ProfileViewModel(authRepository, userRepository, subscriptionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Test User", state.name)
        assertEquals("user@thechumbak.com", state.email)
        assertTrue(state.isAdmin)
        assertTrue(state.hasActiveSubscription)
        assertEquals("Active", state.subscriptionStatus)
        assertEquals("All Access VIP", state.planName)
        assertNotNull(state.expiryDate)
    }
}
