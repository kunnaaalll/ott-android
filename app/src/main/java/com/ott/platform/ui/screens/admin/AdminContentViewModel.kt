package com.ott.platform.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.model.Content
import com.ott.platform.data.model.ContentStatus
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.ContentRepository
import com.ott.platform.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.ott.platform.data.repository.MediaUploadRepository
import com.ott.platform.data.model.MediaType
import com.ott.platform.data.model.MediaSource
import kotlinx.coroutines.flow.catch

@HiltViewModel
class AdminContentViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val contentRepository: ContentRepository,
    private val mediaUploadRepository: MediaUploadRepository
) : ViewModel() {

    private val _contentList = MutableStateFlow<List<Content>>(emptyList())
    val contentList: StateFlow<List<Content>> = _contentList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val uploadProgress: StateFlow<Map<String, Int>> = _uploadProgress.asStateFlow()

    private var currentUser: User? = null

    init {
        loadAdminData()
    }

    private fun loadAdminData() {
        viewModelScope.launch {
            _isLoading.value = true
            val fbUser = authRepository.currentUser.firstOrNull()
            if (fbUser != null) {
                val user = userRepository.observeUser(fbUser.uid).firstOrNull()
                if (user != null && user.isAdmin) {
                    currentUser = user
                    observeAllContent(user.tenantId)
                } else {
                    _errorMessage.value = "Unauthorized"
                    _isLoading.value = false
                }
            } else {
                _errorMessage.value = "Unauthenticated"
                _isLoading.value = false
            }
        }
    }

    private fun observeAllContent(tenantId: String) {
        viewModelScope.launch {
            try {
                contentRepository.observeAllContentAdmin(tenantId).collectLatest {
                    _contentList.value = it
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load content: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    fun publishContent(contentId: String) {
        updateStatus(contentId, ContentStatus.PUBLISHED.name)
    }

    fun unpublishContent(contentId: String) {
        updateStatus(contentId, ContentStatus.DRAFT.name)
    }

    fun archiveContent(contentId: String) {
        updateStatus(contentId, ContentStatus.ARCHIVED.name)
    }

    private fun updateStatus(contentId: String, status: String) {
        viewModelScope.launch {
            try {
                contentRepository.updateContentStatus(contentId, status)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update status: ${e.localizedMessage}"
            }
        }
    }

    fun createOrUpdateContent(content: Content) {
        val user = currentUser ?: return
        
        // Tenant security check: Admin can only create/update content for their own tenant
        if (content.tenantId.isNotEmpty() && content.tenantId != user.tenantId) {
            _errorMessage.value = "Permission denied: Cannot modify another tenant's content"
            return
        }

        // Enforce tenant ID
        val securedContent = content.copy(tenantId = user.tenantId)

        // Validation
        if (securedContent.title.isBlank()) {
            _errorMessage.value = "Title is required"
            return
        }
        if (securedContent.description.isBlank()) {
            _errorMessage.value = "Description is required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                contentRepository.saveContent(securedContent)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save content: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun uploadMedia(contentId: String, mediaType: MediaType, source: MediaSource) {
        viewModelScope.launch {
            try {
                // 1. Request Authorization
                val fileName = source.displayName ?: "unknown_file"
                val mime = source.mimeType ?: "application/octet-stream"
                val size = source.sizeBytes ?: 0L

                val auth = mediaUploadRepository.requestUploadAuthorization(
                    contentId = contentId,
                    mediaType = mediaType.name,
                    fileName = fileName,
                    mimeType = mime,
                    fileSize = size
                )

                // 2. Upload with progress
                mediaUploadRepository.uploadMedia(source, auth.signedUrl)
                    .catch { e ->
                        _errorMessage.value = "Upload failed: ${e.message}"
                        _uploadProgress.value = _uploadProgress.value - mediaType.name
                    }
                    .collectLatest { progress ->
                        _uploadProgress.value = _uploadProgress.value + (mediaType.name to progress)
                        if (progress == 100) {
                            // 3. Complete Upload
                            val finalStatus = mediaUploadRepository.completeUpload(auth.uploadId)
                            if (finalStatus == "FAILED") {
                                _errorMessage.value = "Processing failed for ${mediaType.name}"
                            }
                            _uploadProgress.value = _uploadProgress.value - mediaType.name
                        }
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upload media: ${e.localizedMessage}"
            }
        }
    }
}
