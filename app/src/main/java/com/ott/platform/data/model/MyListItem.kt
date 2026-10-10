package com.ott.platform.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentId

data class MyListItem(
    @DocumentId
    val id: String = "",
    val contentId: String = "",
    @ServerTimestamp
    val addedAt: Timestamp? = null
)
