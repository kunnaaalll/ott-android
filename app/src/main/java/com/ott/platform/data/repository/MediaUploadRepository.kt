package com.ott.platform.data.repository

import com.ott.platform.data.model.MediaUpload
import com.ott.platform.data.model.MediaSource
import kotlinx.coroutines.flow.Flow

interface MediaUploadRepository {
    
    /** Request short-lived upload authorization */
    suspend fun requestUploadAuthorization(
        contentId: String,
        mediaType: String,
        fileName: String,
        mimeType: String,
        fileSize: Long
    ): UploadAuthorization

    /** 
     * Perform direct upload to S3 using the signed URL. 
     * Emits progress percentage (0-100).
     */
    fun uploadMedia(
        source: MediaSource,
        signedUrl: String
    ): Flow<Int>

    /** Confirm upload completion with backend */
    suspend fun completeUpload(uploadId: String): String

    /** Observe the status of an upload from Firestore */
    fun observeUploadStatus(uploadId: String): Flow<MediaUpload?>
}

data class UploadAuthorization(
    val uploadId: String,
    val signedUrl: String,
    val sourceKey: String
)
