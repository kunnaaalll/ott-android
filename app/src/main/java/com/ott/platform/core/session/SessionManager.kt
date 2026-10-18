package com.ott.platform.core.session

import android.content.Context
import com.ott.platform.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ott_user_session", Context.MODE_PRIVATE)

    private val _currentUserState = MutableStateFlow<User?>(loadCachedUser())
    val currentUserState: StateFlow<User?> = _currentUserState.asStateFlow()

    fun saveSession(user: User) {
        prefs.edit()
            .putString("uid", user.id)
            .putString("email", user.email)
            .putString("name", user.name)
            .putString("tenantId", user.tenantId)
            .putBoolean("isAdmin", user.isAdmin)
            .apply()
        _currentUserState.value = user
    }

    fun getUserId(): String? {
        val uid = prefs.getString("uid", null)
        return if (!uid.isNullOrBlank()) uid else _currentUserState.value?.id?.takeIf { it.isNotBlank() }
    }

    fun getSessionUser(): User? {
        return _currentUserState.value
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _currentUserState.value = null
    }

    fun isLoggedIn(): Boolean {
        return !getUserId().isNullOrBlank()
    }

    private fun loadCachedUser(): User? {
        val uid = prefs.getString("uid", null) ?: return null
        val email = prefs.getString("email", "") ?: ""
        val name = prefs.getString("name", "") ?: ""
        val tenantId = prefs.getString("tenantId", "tenant_chumbak") ?: "tenant_chumbak"
        val isAdmin = prefs.getBoolean("isAdmin", false)
        return User(
            id = uid,
            email = email,
            name = name,
            tenantId = tenantId,
            isAdmin = isAdmin
        )
    }
}
