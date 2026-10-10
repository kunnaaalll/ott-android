package com.ott.platform.data.repository.impl

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class MediaUploadRepositoryImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var context: Context
    private lateinit var repository: MediaUploadRepositoryImpl

    @Before
    fun setup() {
        firestore = mock(FirebaseFirestore::class.java)
        functions = mock(FirebaseFunctions::class.java)
        context = mock(Context::class.java)

        repository = MediaUploadRepositoryImpl(firestore, functions, context)
    }

    @Test
    fun `requestUploadAuthorization calls functions and maps response`() = runTest {
        val callable = mock(HttpsCallableReference::class.java)
        val result = mock(HttpsCallableResult::class.java)
        val responseData = mapOf(
            "uploadId" to "upload_123",
            "signedUrl" to "https://s3.amazonaws.com/test",
            "sourceKey" to "uploads/tenant/sourceKey"
        )

        `when`(functions.getHttpsCallable("createMediaUpload")).thenReturn(callable)
        `when`(callable.call(any())).thenReturn(Tasks.forResult(result))
        `when`(result.data).thenReturn(responseData)

        val auth = repository.requestUploadAuthorization(
            contentId = "content_1",
            mediaType = "MAIN_VIDEO",
            fileName = "movie.mp4",
            mimeType = "video/mp4",
            fileSize = 1000L
        )

        assertEquals("upload_123", auth.uploadId)
        assertEquals("https://s3.amazonaws.com/test", auth.signedUrl)
    }
}
