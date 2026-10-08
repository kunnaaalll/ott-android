package com.ott.platform.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore: tenants/{tenantId}
 *
 * Represents a single OTT customer/brand on the platform.
 * The Android app is configured for one tenant via BuildConfig.TENANT_ID.
 */
data class Tenant(
    @DocumentId
    val id: String = "",

    val name: String = "",
    val slug: String = "",

    @PropertyName("logoUrl")
    val logoUrl: String = "",

    @PropertyName("primaryColor")
    val primaryColor: String = "",

    @PropertyName("secondaryColor")
    val secondaryColor: String = "",

    @ServerTimestamp
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    val status: String = "active",
)
