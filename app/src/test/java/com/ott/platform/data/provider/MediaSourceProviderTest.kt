package com.ott.platform.data.provider

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class MediaSourceProviderTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var uri: Uri
    private lateinit var cursor: Cursor
    private lateinit var mediaSourceProvider: MediaSourceProvider

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        uri = mockk(relaxed = true)
        cursor = mockk(relaxed = true)

        every { context.contentResolver } returns contentResolver
        mediaSourceProvider = MediaSourceProvider(context)
    }

    @Test
    fun `getMediaSource extracts metadata correctly`() {
        every { contentResolver.getType(uri) } returns "video/mp4"
        every { contentResolver.query(uri, null, null, null, null) } returns cursor
        
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.getColumnIndex(OpenableColumns.SIZE) } returns 1
        
        every { cursor.getString(0) } returns "test_video.mp4"
        every { cursor.getLong(1) } returns 1048576L

        val source = mediaSourceProvider.getMediaSource(uri)

        assertEquals("video/mp4", source.mimeType)
        assertEquals("test_video.mp4", source.displayName)
        assertEquals(1048576L, source.sizeBytes)
        
        verify { cursor.close() }
    }

    @Test
    fun `getMediaSource handles inaccessible URI safely`() {
        every { contentResolver.getType(uri) } returns null
        every { contentResolver.query(uri, null, null, null, null) } returns null

        val source = mediaSourceProvider.getMediaSource(uri)

        assertNull(source.mimeType)
        assertNull(source.displayName)
        assertNull(source.sizeBytes)
    }
}
