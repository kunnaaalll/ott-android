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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
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
        firestore = mock(FirebaseFirestore::class.java)
        collectionReference = mock(CollectionReference::class.java)
        query = mock(Query::class.java)
        querySnapshot = mock(QuerySnapshot::class.java)
        
        `when`(firestore.collection(anyString())).thenReturn(collectionReference)
        `when`(collectionReference.whereEqualTo(anyString(), any())).thenReturn(query)
        `when`(query.whereEqualTo(anyString(), any())).thenReturn(query)
        
        repository = SubscriptionRepositoryImpl(firestore)
    }

    @Test
    fun `getPlans returns list of active plans`() = runTest {
        val mockPlan = Plan(id = "plan1", name = "Premium", price = 199.0, active = true)
        `when`(query.get()).thenReturn(Tasks.forResult(querySnapshot))
        `when`(querySnapshot.toObjects(Plan::class.java)).thenReturn(listOf(mockPlan))

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
        
        `when`(query.get()).thenReturn(Tasks.forResult(querySnapshot))
        `when`(querySnapshot.toObjects(Subscription::class.java)).thenReturn(listOf(mockSub))

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
        
        `when`(query.get()).thenReturn(Tasks.forResult(querySnapshot))
        `when`(querySnapshot.toObjects(Subscription::class.java)).thenReturn(listOf(mockSub))

        val isActive = repository.isSubscriptionActive("user1", "tenant1")

        assertFalse(isActive)
    }

    @Test
    fun `isSubscriptionActive returns false when there are no subscriptions`() = runTest {
        `when`(query.get()).thenReturn(Tasks.forResult(querySnapshot))
        `when`(querySnapshot.toObjects(Subscription::class.java)).thenReturn(emptyList())

        val isActive = repository.isSubscriptionActive("user1", "tenant1")

        assertFalse(isActive)
    }
}
