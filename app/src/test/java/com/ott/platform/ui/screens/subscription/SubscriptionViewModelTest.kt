package com.ott.platform.ui.screens.subscription

import com.ott.platform.core.api.CheckoutHandoff
import com.ott.platform.core.api.NotSignedInException
import com.ott.platform.core.payment.CheckoutConfirmation
import com.ott.platform.core.payment.CheckoutConfirmationService
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

import com.ott.platform.core.analytics.MetaAnalyticsTracker
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: com.ott.platform.data.repository.UserRepository
    private lateinit var paymentGateway: PaymentGateway
    private lateinit var metaAnalyticsTracker: MetaAnalyticsTracker
    private lateinit var viewModel: SubscriptionViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        subscriptionRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        paymentGateway = mockk(relaxed = true)
        metaAnalyticsTracker = mockk(relaxed = true)
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

        viewModel = SubscriptionViewModel(subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.plans.value.size)
        assertTrue(viewModel.subscriptionState.value is SubscriptionState.Inactive)
    }

    @Test
    fun `subscription state becomes active when user has active subscription and emits Meta purchase event`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        val firebaseUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "user1"
        }
        val plan = Plan(id = "plan1", name = "Monthly Pass", price = 299.0)
        val sub = Subscription(
            id = "sub_order_12345",
            planId = "plan1",
            status = SubscriptionStatus.ACTIVE.name, 
            expiryDate = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 86400000))
        )
        
        coEvery { authRepository.currentUser } returns flowOf(firebaseUser)
        coEvery { userRepository.observeUser("user1") } returns flowOf(user)
        coEvery { subscriptionRepository.getPlans("tenant1") } returns listOf(plan)
        coEvery { subscriptionRepository.observeSubscription("user1", "tenant1") } returns flowOf(sub)

        viewModel = SubscriptionViewModel(subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.subscriptionState.value is SubscriptionState.Active)
        verify {
            metaAnalyticsTracker.logPurchase(
                orderId = "order_12345",
                planId = "plan1",
                planName = "Monthly Pass",
                amount = 299.0,
                currency = "INR"
            )
        }
    }

    private val handoff = CheckoutHandoff(
        url = "https://thechumbak.com/checkout/handoff#t=" + "A".repeat(43),
        planId = "plan1",
        expiresAtMs = 0L
    )

    private fun otpSessionManager(user: User) = mockk<com.ott.platform.core.session.SessionManager>(relaxed = true) {
        every { getSessionUser() } returns user
        every { getUserId() } returns user.id
    }

    private fun stubSignedInUser(user: User, plans: List<Plan>) {
        coEvery { authRepository.currentUser } returns flowOf(null)
        every { authRepository.getCurrentUserId() } returns user.id
        coEvery { subscriptionRepository.getPlans(user.tenantId) } returns plans
        coEvery { subscriptionRepository.observeSubscription(user.id, user.tenantId) } returns flowOf(null)
    }

    @Test
    fun `initiatePayment prevents duplicate calls`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        val plan = Plan(id = "plan1")
        stubSignedInUser(user, listOf(plan))
        coEvery { paymentGateway.startWebCheckout(plan) } returns handoff

        viewModel = SubscriptionViewModel(
            subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker,
            sessionManager = otpSessionManager(user)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initiatePayment(plan)
        viewModel.initiatePayment(plan)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { paymentGateway.startWebCheckout(plan) }
    }

    @Test
    fun `initiatePayment emits the backend handoff url for the browser and remembers the pending checkout`() = runTest {
        val user = User(id = "usr_mobile_101", tenantId = "tenant_chumbak", name = "Mobile Subscriber")
        val plan = Plan(id = "plan_trial_package", price = 199.0)
        stubSignedInUser(user, listOf(plan))
        coEvery { paymentGateway.startWebCheckout(plan) } returns handoff.copy(planId = plan.id)
        val sessions = otpSessionManager(user)

        viewModel = SubscriptionViewModel(
            subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker,
            sessionManager = sessions
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initiatePayment(plan)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(handoff.url, withTimeoutOrNull(1000) { viewModel.checkoutLaunch.first() })
        verify { sessions.savePendingCheckout(plan.id, any()) }
        assertFalse(viewModel.subscriptionState.value is SubscriptionState.Error)
        // Starting checkout must never mark the user as subscribed.
        assertFalse(viewModel.subscriptionState.value is SubscriptionState.Active)
        coVerify(exactly = 0) { subscriptionRepository.recordActiveSubscription(any()) }
    }

    @Test
    fun `initiatePayment requires a signed in user and never calls the backend without one`() = runTest {
        coEvery { authRepository.currentUser } returns flowOf(null)
        every { authRepository.getCurrentUserId() } returns null
        coEvery { subscriptionRepository.getPlans(any()) } returns emptyList()

        viewModel = SubscriptionViewModel(
            subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initiatePayment(Plan(id = "plan1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.subscriptionState.value is SubscriptionState.Error)
        coVerify(exactly = 0) { paymentGateway.startWebCheckout(any()) }
    }

    @Test
    fun `an expired app session signs the user out instead of opening checkout`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        val plan = Plan(id = "plan1")
        stubSignedInUser(user, listOf(plan))
        coEvery { paymentGateway.startWebCheckout(plan) } throws NotSignedInException()

        viewModel = SubscriptionViewModel(
            subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker,
            sessionManager = otpSessionManager(user)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initiatePayment(plan)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.subscriptionState.value is SubscriptionState.Error)
        coVerify(exactly = 1) { authRepository.signOut() }
        assertNull(withTimeoutOrNull(100) { viewModel.checkoutLaunch.first() })
    }

    @Test
    fun `returning from checkout only becomes Active when the backend confirms`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        val plan = Plan(id = "plan1", name = "Monthly", price = 279.0)
        stubSignedInUser(user, listOf(plan))
        val confirmation = mockk<CheckoutConfirmationService>()
        val confirmed = Subscription(
            id = "sub_order_9",
            userId = "user1",
            planId = "plan1",
            status = SubscriptionStatus.ACTIVE.name,
            currentPeriodEnd = System.currentTimeMillis() + 86_400_000L,
            providerSubscriptionId = "order_9"
        )
        coEvery { confirmation.confirm("order_9", any(), any()) } returns CheckoutConfirmation.Confirmed(confirmed)

        viewModel = SubscriptionViewModel(
            subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker,
            checkoutConfirmation = confirmation, sessionManager = otpSessionManager(user)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleCheckoutReturn("order_9")
        assertTrue(viewModel.subscriptionState.value is SubscriptionState.Confirming)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SubscriptionState.Active(confirmed), viewModel.subscriptionState.value)
    }

    @Test
    fun `backend unavailable or unpaid keeps the state pending and never fabricates a subscription`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        val plan = Plan(id = "plan1")
        stubSignedInUser(user, listOf(plan))
        val confirmation = mockk<CheckoutConfirmationService>()
        coEvery { confirmation.confirm(any(), any(), any()) } returns CheckoutConfirmation.Pending

        viewModel = SubscriptionViewModel(
            subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker,
            checkoutConfirmation = confirmation, sessionManager = otpSessionManager(user)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleCheckoutReturn("order_forged_in_a_deep_link")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SubscriptionState.ConfirmingTimeout, viewModel.subscriptionState.value)
        coVerify(exactly = 0) { subscriptionRepository.recordActiveSubscription(any()) }
        verify(exactly = 0) { metaAnalyticsTracker.logPurchase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a payment the backend reports as not completed shows a retryable error`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        stubSignedInUser(user, listOf(Plan(id = "plan1")))
        val confirmation = mockk<CheckoutConfirmationService>()
        coEvery { confirmation.confirm(any(), any(), any()) } returns CheckoutConfirmation.Failed
        val sessions = otpSessionManager(user)

        viewModel = SubscriptionViewModel(
            subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker,
            checkoutConfirmation = confirmation, sessionManager = sessions
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.retryCheckPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.subscriptionState.value is SubscriptionState.Error)
        verify { sessions.clearPendingCheckout() }
    }

    @Test
    fun `resuming the screen re-checks with the backend only after a checkout was started`() = runTest {
        val user = User(id = "user1", tenantId = "tenant1")
        stubSignedInUser(user, listOf(Plan(id = "plan1")))
        val confirmation = mockk<CheckoutConfirmationService>()
        coEvery { confirmation.confirm(any(), any(), any()) } returns CheckoutConfirmation.Pending
        val sessions = otpSessionManager(user)

        viewModel = SubscriptionViewModel(
            subscriptionRepository, authRepository, userRepository, paymentGateway, metaAnalyticsTracker,
            checkoutConfirmation = confirmation, sessionManager = sessions
        )
        testDispatcher.scheduler.advanceUntilIdle()

        every { sessions.hasRecentPendingCheckout(any(), any()) } returns false
        viewModel.onScreenResumed()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { confirmation.confirm(any(), any(), any()) }

        every { sessions.hasRecentPendingCheckout(any(), any()) } returns true
        viewModel.onScreenResumed()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { confirmation.confirm(null, any(), any()) }
    }
}
