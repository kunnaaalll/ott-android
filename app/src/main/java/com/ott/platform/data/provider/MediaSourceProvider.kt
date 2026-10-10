package com.ott.platform.data.provider

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ott.platform.data.model.MediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSourceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getMediaSource(uri: Uri): MediaSource {
        var displayName: String? = null
        var sizeBytes: Long? = null
        val mimeType = context.contentResolver.getType(uri)

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex)
                }
                if (sizeIndex != -1) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }
        return MediaSource(uri, displayName, mimeType, sizeBytes)
    }
}
