package com.ott.platform.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _publishedCount = MutableStateFlow(0)
    val publishedCount: StateFlow<Int> = _publishedCount.asStateFlow()

    private val _draftCount = MutableStateFlow(0)
    val draftCount: StateFlow<Int> = _draftCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadDashboardStats()
    }

    private fun loadDashboardStats() {
        viewModelScope.launch {
            _isLoading.value = true
            val fbUser = authRepository.currentUser.firstOrNull()
            if (fbUser != null) {
                val user = userRepository.observeUser(fbUser.uid).firstOrNull()
                if (user != null && user.isAdmin) {
                    observeStats(user.tenantId)
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

    private fun observeStats(tenantId: String) {
        viewModelScope.launch {
            try {
                contentRepository.observeAllContentAdmin(tenantId).collectLatest { contentList ->
                    _publishedCount.value = contentList.count { it.status == ContentStatus.PUBLISHED.name }
                    _draftCount.value = contentList.count { it.status == ContentStatus.DRAFT.name }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load stats: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }
}
