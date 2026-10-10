package com.ott.platform.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Category(
    @DocumentId
    val id: String = "",

    @PropertyName("tenantId")
    val tenantId: String = "",

    val name: String = "",
    val slug: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0
)
