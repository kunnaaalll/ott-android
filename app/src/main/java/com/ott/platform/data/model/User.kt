package com.ott.platform.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore: users/{uid}
 *
 * Created on first login. The document ID matches the Firebase Auth UID.
 * The tenantId links the user to their OTT brand.
 */
data class User(
    @DocumentId
    val id: String = "",

    @PropertyName("tenantId")
    val tenantId: String = "",

    val email: String = "",
    val name: String = "",

    @PropertyName("photoUrl")
    val photoUrl: String = "",

    @ServerTimestamp
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @PropertyName("lastLoginAt")
    val lastLoginAt: Timestamp? = null,
)
