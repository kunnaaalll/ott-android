package com.ott.platform.data.model

import android.net.Uri

data class MediaSource(
    val uri: Uri,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?
)
