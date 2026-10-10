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
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery

@OptIn(ExperimentalCoroutinesApi::class)
class MediaUploadRepositoryImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var context: Context
    private lateinit var repository: MediaUploadRepositoryImpl

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        functions = mockk(relaxed = true)
        context = mockk(relaxed = true)

        repository = MediaUploadRepositoryImpl(firestore, functions, context)
    }

    @Test
    fun `requestUploadAuthorization calls functions and maps response`() = runTest {
        val callable = mockk<HttpsCallableReference>(relaxed = true)
        val result = mockk<HttpsCallableResult>(relaxed = true)
        val responseData = mapOf(
            "uploadId" to "upload_123",
            "signedUrl" to "https://s3.amazonaws.com/test",
            "sourceKey" to "uploads/tenant/sourceKey"
        )

        every { functions.getHttpsCallable("createMediaUpload") } returns callable
        every { callable.call(any()) } returns Tasks.forResult(result)
        every { result.data } returns responseData

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
