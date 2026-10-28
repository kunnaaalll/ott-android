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
        val emailDigits = user.email.filter { it.isDigit() }
        val editor = prefs.edit()
            .putString("uid", user.id)
            .putString("email", user.email)
            .putString("name", user.name)
            .putString("tenantId", user.tenantId)
            .putBoolean("isAdmin", user.isAdmin)
        if (emailDigits.length >= 10) {
            editor.putString("saved_phone_number", emailDigits.takeLast(10))
        }
        editor.apply()
        _currentUserState.value = user
    }

    fun savePhoneNumber(phone: String) {
        val clean = phone.filter { it.isDigit() }.takeLast(10)
        if (clean.length == 10) {
            prefs.edit().putString("saved_phone_number", clean).apply()
        }
    }

    fun getPhoneNumber(): String? {
        val direct = prefs.getString("saved_phone_number", null)?.takeIf { it.isNotBlank() }
        if (!direct.isNullOrBlank()) return direct
        val email = prefs.getString("email", "") ?: ""
        val digits = email.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else null
    }

    fun getUserId(): String? {
        val uid = prefs.getString("uid", null)
        return if (!uid.isNullOrBlank()) uid else _currentUserState.value?.id?.takeIf { it.isNotBlank() }
    }

    fun getSessionUser(): User? {
        return _currentUserState.value ?: loadCachedUser()
    }

    fun clearSession() {
        prefs.edit()
            .remove("uid")
            .remove("email")
            .remove("name")
            .remove("tenantId")
            .remove("isAdmin")
            .remove("pending_order_id")
            .remove("pending_plan_id")
            .remove("pending_checkout_at")
            .apply()
        _currentUserState.value = null
    }

    fun isLoggedIn(): Boolean {
        return !getUserId().isNullOrBlank()
    }

    fun savePendingOrderId(orderId: String, planId: String = "") {
        prefs.edit()
            .putString("pending_order_id", orderId)
            .putString("pending_plan_id", planId)
            .apply()
    }

    fun getPendingOrderId(): String? {
        return prefs.getString("pending_order_id", null)?.takeIf { it.isNotBlank() }
    }

    fun getPendingPlanId(): String? {
        return prefs.getString("pending_plan_id", null)?.takeIf { it.isNotBlank() }
    }

    fun clearPendingOrderId() {
        prefs.edit()
            .remove("pending_order_id")
            .remove("pending_plan_id")
            .apply()
    }

    /**
     * Remembers that the user was sent to the website checkout for [planId]. This is only a hint
     * to re-check with the backend when the user comes back; it is never evidence of payment.
     */
    fun savePendingCheckout(planId: String, nowMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString("pending_plan_id", planId)
            .putLong("pending_checkout_at", nowMs)
            .apply()
    }

    fun hasRecentPendingCheckout(
        nowMs: Long = System.currentTimeMillis(),
        windowMs: Long = PENDING_CHECKOUT_WINDOW_MS
    ): Boolean {
        val startedAt = prefs.getLong("pending_checkout_at", 0L)
        return startedAt > 0L && nowMs - startedAt in 0..windowMs
    }

    fun clearPendingCheckout() {
        prefs.edit()
            .remove("pending_order_id")
            .remove("pending_plan_id")
            .remove("pending_checkout_at")
            .apply()
    }

    private companion object {
        /** A checkout marker older than this is ignored (the user abandoned the attempt). */
        const val PENDING_CHECKOUT_WINDOW_MS = 30L * 60L * 1000L
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
