package com.ott.platform.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class MediaType {
    MAIN_VIDEO,
    TRAILER,
    POSTER,
    BACKDROP
}

enum class UploadStatus {
    PENDING_UPLOAD,
    UPLOADING,
    UPLOADED,
    PROCESSING,
    READY,
    FAILED,
    CANCELLED;

    companion object {
        fun fromString(value: String): UploadStatus {
            return values().find { it.name == value } ?: PENDING_UPLOAD
        }
    }
}

data class MediaUpload(
    @DocumentId
    val id: String = "",
    val contentId: String = "",
    val tenantId: String = "",
    val mediaType: String = MediaType.MAIN_VIDEO.name,
    val sourceKey: String = "",
    val status: String = UploadStatus.PENDING_UPLOAD.name,
    val createdBy: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val errorCode: String? = null,

    @ServerTimestamp
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @PropertyName("updatedAt")
    val updatedAt: Timestamp? = null
)
