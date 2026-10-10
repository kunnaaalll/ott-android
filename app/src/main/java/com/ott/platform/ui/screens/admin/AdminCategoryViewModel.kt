package com.ott.platform.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.model.Category
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.CategoryRepository
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
class AdminCategoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _categoryList = MutableStateFlow<List<Category>>(emptyList())
    val categoryList: StateFlow<List<Category>> = _categoryList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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
                    observeCategories(user.tenantId)
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

    private fun observeCategories(tenantId: String) {
        viewModelScope.launch {
            try {
                categoryRepository.observeAllCategories(tenantId).collectLatest {
                    _categoryList.value = it
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load categories: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    fun saveCategory(category: Category) {
        val user = currentUser ?: return
        
        if (category.tenantId.isNotEmpty() && category.tenantId != user.tenantId) {
            _errorMessage.value = "Permission denied"
            return
        }

        val securedCategory = category.copy(tenantId = user.tenantId)

        if (securedCategory.name.isBlank()) {
            _errorMessage.value = "Name is required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                categoryRepository.saveCategory(securedCategory)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save category: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deactivateCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(categoryId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to deactivate: ${e.localizedMessage}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
