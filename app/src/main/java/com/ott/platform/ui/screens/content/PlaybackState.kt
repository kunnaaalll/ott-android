package com.ott.platform.ui.screens.content

sealed class PlaybackState {
    object Idle : PlaybackState()
    object Authorizing : PlaybackState()
    object Preparing : PlaybackState()
    object Playing : PlaybackState()
    object Paused : PlaybackState()
    object Buffering : PlaybackState()
    object Completed : PlaybackState()
    data class Error(val message: String, val reason: String? = null) : PlaybackState()
}
