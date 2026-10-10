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
import org.mockito.Mockito.*

class MediaSourceProviderTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var uri: Uri
    private lateinit var cursor: Cursor
    private lateinit var mediaSourceProvider: MediaSourceProvider

    @Before
    fun setup() {
        context = mock(Context::class.java)
        contentResolver = mock(ContentResolver::class.java)
        uri = mock(Uri::class.java)
        cursor = mock(Cursor::class.java)

        `when`(context.contentResolver).thenReturn(contentResolver)
        mediaSourceProvider = MediaSourceProvider(context)
    }

    @Test
    fun `getMediaSource extracts metadata correctly`() {
        `when`(contentResolver.getType(uri)).thenReturn("video/mp4")
        `when`(contentResolver.query(uri, null, null, null, null)).thenReturn(cursor)
        
        `when`(cursor.moveToFirst()).thenReturn(true)
        `when`(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)).thenReturn(0)
        `when`(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(1)
        
        `when`(cursor.getString(0)).thenReturn("test_video.mp4")
        `when`(cursor.getLong(1)).thenReturn(1048576L)

        val source = mediaSourceProvider.getMediaSource(uri)

        assertEquals("video/mp4", source.mimeType)
        assertEquals("test_video.mp4", source.displayName)
        assertEquals(1048576L, source.sizeBytes)
        
        verify(cursor).close()
    }

    @Test
    fun `getMediaSource handles inaccessible URI safely`() {
        `when`(contentResolver.getType(uri)).thenReturn(null)
        `when`(contentResolver.query(uri, null, null, null, null)).thenReturn(null)

        val source = mediaSourceProvider.getMediaSource(uri)

        assertNull(source.mimeType)
        assertNull(source.displayName)
        assertNull(source.sizeBytes)
    }
}
