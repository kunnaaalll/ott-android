package com.ott.platform.data.repository.impl

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.ott.platform.data.model.MediaUpload
import com.ott.platform.data.model.MediaSource
import com.ott.platform.data.repository.MediaUploadRepository
import com.ott.platform.data.repository.UploadAuthorization
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUploadRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    @ApplicationContext private val context: Context
) : MediaUploadRepository {

    override suspend fun requestUploadAuthorization(
        contentId: String,
        mediaType: String,
        fileName: String,
        mimeType: String,
        fileSize: Long
    ): UploadAuthorization {
        val data = hashMapOf(
            "contentId" to contentId,
            "mediaType" to mediaType,
            "fileName" to fileName,
            "mimeType" to mimeType,
            "fileSize" to fileSize
        )

        val result = functions
            .getHttpsCallable("createMediaUpload")
            .call(data)
            .await()
            .data as Map<*, *>

        return UploadAuthorization(
            uploadId = result["uploadId"] as String,
            signedUrl = result["signedUrl"] as String,
            sourceKey = result["sourceKey"] as String
        )
    }

    override fun uploadMedia(
        source: MediaSource,
        signedUrl: String
    ): Flow<Int> = flow {
        val url = URL(signedUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.doOutput = true
        connection.requestMethod = "PUT"
        source.mimeType?.let { connection.setRequestProperty("Content-Type", it) }
        
        val fileSize = source.sizeBytes ?: -1L
        if (fileSize > 0) {
            connection.setRequestProperty("Content-Length", fileSize.toString())
            connection.setFixedLengthStreamingMode(fileSize)
        } else {
            connection.setChunkedStreamingMode(8192)
        }

        var uploadedBytes = 0L

        context.contentResolver.openInputStream(source.uri)?.use { inputStream ->
            connection.outputStream.use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    uploadedBytes += bytesRead
                    if (fileSize > 0) {
                        val progress = ((uploadedBytes.toDouble() / fileSize) * 100).toInt()
                        emit(progress)
                    } else {
                        emit(-1)
                    }
                }
                outputStream.flush()
            }
        } ?: throw Exception("Could not open InputStream for URI: ${source.uri}")

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw Exception("Upload failed with HTTP code $responseCode")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun completeUpload(uploadId: String): String {
        val data = hashMapOf("uploadId" to uploadId)
        val result = functions
            .getHttpsCallable("completeMediaUpload")
            .call(data)
            .await()
            .data as Map<*, *>
        
        return result["status"] as String
    }

    override fun observeUploadStatus(uploadId: String): Flow<MediaUpload?> = callbackFlow {
        val listener = firestore.collection("mediaUploads")
            .document(uploadId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val upload = snapshot?.toObject(MediaUpload::class.java)
                trySend(upload)
            }
        awaitClose { listener.remove() }
    }
}
