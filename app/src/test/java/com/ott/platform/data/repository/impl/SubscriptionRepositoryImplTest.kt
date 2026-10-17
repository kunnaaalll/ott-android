package com.ott.platform.data.repository.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.SubscriptionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import io.mockk.every
import io.mockk.mockk
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionRepositoryImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: SubscriptionRepositoryImpl
    private lateinit var collectionReference: CollectionReference
    private lateinit var query: Query
    private lateinit var querySnapshot: QuerySnapshot

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        collectionReference = mockk(relaxed = true)
        query = mockk(relaxed = true)
        querySnapshot = mockk(relaxed = true)
        
        every { firestore.collection(any()) } returns collectionReference
        every { collectionReference.whereEqualTo(any<String>(), any()) } returns query
        every { query.whereEqualTo(any<String>(), any()) } returns query
        
        repository = SubscriptionRepositoryImpl(firestore)
    }

    @Test
    fun `getPlans returns list of active plans`() = runTest {
        val mockPlan = Plan(id = "plan1", name = "Premium", price = 199.0, active = true)
        every { query.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.toObjects(Plan::class.java) } returns listOf(mockPlan)

        val plans = repository.getPlans("tenant1")

        assertEquals(1, plans.size)
        assertEquals("Premium", plans[0].name)
    }

    @Test
    fun `isSubscriptionActive returns true when there is an active subscription`() = runTest {
        val futureDate = Date(System.currentTimeMillis() + 86400000) // +1 day
        val mockSub = Subscription(
            status = SubscriptionStatus.ACTIVE.name,
            expiryDate = Timestamp(futureDate)
        )
        
        every { query.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.toObjects(Subscription::class.java) } returns listOf(mockSub)

        val isActive = repository.isSubscriptionActive("user1", "tenant1")

        assertTrue(isActive)
    }

    @Test
    fun `isSubscriptionActive returns false when subscription is expired`() = runTest {
        val pastDate = Date(System.currentTimeMillis() - 86400000) // -1 day
        val mockSub = Subscription(
            status = SubscriptionStatus.ACTIVE.name,
            expiryDate = Timestamp(pastDate)
        )
        
        every { query.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.toObjects(Subscription::class.java) } returns listOf(mockSub)

        val isActive = repository.isSubscriptionActive("user1", "tenant1")

        assertFalse(isActive)
    }

    @Test
    fun `isSubscriptionActive returns false when there are no subscriptions`() = runTest {
        every { query.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.toObjects(Subscription::class.java) } returns emptyList()

        val isActive = repository.isSubscriptionActive("user1", "tenant1")

        assertFalse(isActive)
    }

    @Test
    fun `getPlans falls back to PlanDefaults when firestore returns empty list`() = runTest {
        every { query.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.toObjects(Plan::class.java) } returns emptyList()

        val plans = repository.getPlans("tenant1")

        assertEquals(6, plans.size)
        assertEquals("plan_year_vip", plans[0].id)
        assertEquals(599.0, plans[0].price, 0.001)
    }

    @Test
    fun `recordActiveSubscription updates local state and makes subscription active`() = runTest {
        val futureEnd = System.currentTimeMillis() + 86400000L
        val sub = Subscription(
            id = "sub_test",
            userId = "user1",
            status = SubscriptionStatus.ACTIVE.name,
            currentPeriodEnd = futureEnd
        )

        repository.recordActiveSubscription(sub)
        val isActive = repository.isSubscriptionActive("user1", "tenant1")

        assertTrue(isActive)
    }
}

