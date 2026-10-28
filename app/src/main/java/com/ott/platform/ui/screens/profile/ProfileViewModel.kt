package com.ott.platform.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.SubscriptionRepository
import com.ott.platform.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val tenantId: String = "",
    val isAdmin: Boolean = false,
    val subscriptionStatus: String = "Inactive",
    val planName: String = "None",
    val expiryDate: String? = null,
    val hasActiveSubscription: Boolean = false,
    val isLoading: Boolean = true,
    val streamingQuality: String = "Full HD (1080p)",
    val wifiOnlyStreaming: Boolean = false,
    val autoplayPreviews: Boolean = true,
    val wifiOnlyDownloads: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val cacheSize: String = "142 MB"
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository,
    @ApplicationContext private val context: Context? = null
) : ViewModel() {

    private val prefs = context?.getSharedPreferences("ott_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            streamingQuality = prefs?.getString("streaming_quality", "Full HD (1080p)") ?: "Full HD (1080p)",
            wifiOnlyStreaming = prefs?.getBoolean("wifi_only_streaming", false) ?: false,
            autoplayPreviews = prefs?.getBoolean("autoplay_previews", true) ?: true,
            wifiOnlyDownloads = prefs?.getBoolean("wifi_only_downloads", true) ?: true,
            notificationsEnabled = prefs?.getBoolean("notifications_enabled", true) ?: true,
            cacheSize = prefs?.getString("cache_size", "142 MB") ?: "142 MB"
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepository.getCurrentUserId()
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            userRepository.observeUser(uid)
                .filterNotNull()
                .collectLatest { user ->
                    val tenantId = user.tenantId.ifBlank { "tenant-default" }

                    val rawEmail = user.email
                    var derivedPhone = ""
                    if (rawEmail.endsWith("@thechumbak.com")) {
                        val prefix = rawEmail.removeSuffix("@thechumbak.com")
                        if (prefix.all { it.isDigit() } && prefix.length >= 10) {
                            val last10 = prefix.takeLast(10)
                            derivedPhone = "+91 ${last10.substring(0, 5)} ${last10.substring(5)}"
                        }
                    }

                    val displayName = if (user.name.isNotBlank()) {
                        user.name
                    } else if (derivedPhone.isNotBlank()) {
                        "User ${derivedPhone.takeLast(4)}"
                    } else {
                        "Member"
                    }

                    // Observe subscription for this user
                    subscriptionRepository.observeSubscription(uid, tenantId)
                        .collectLatest { subscription ->
                            val plans = try {
                                subscriptionRepository.getPlans(tenantId)
                            } catch (e: Exception) {
                                emptyList()
                            }

                            val planName = plans.find { it.id == subscription?.planId }?.name
                                ?: subscription?.planId?.takeIf { it.isNotBlank() }
                                ?: if (subscription?.isActive() == true) "Active Plan" else "None"

                            val expiryFormatted = formatExpiry(subscription)

                            _uiState.update { current ->
                                current.copy(
                                    name = displayName,
                                    email = user.email,
                                    phone = derivedPhone,
                                    tenantId = user.tenantId,
                                    isAdmin = user.isAdmin,
                                    subscriptionStatus = if (subscription?.isActive() == true) "Active" else "Inactive",
                                    planName = planName,
                                    expiryDate = expiryFormatted,
                                    hasActiveSubscription = subscription?.isActive() == true,
                                    isLoading = false
                                )
                            }
                        }
                }
        }
    }

    fun setStreamingQuality(quality: String) {
        prefs?.edit()?.putString("streaming_quality", quality)?.apply()
        _uiState.update { it.copy(streamingQuality = quality) }
    }

    fun setWifiOnlyStreaming(enabled: Boolean) {
        prefs?.edit()?.putBoolean("wifi_only_streaming", enabled)?.apply()
        _uiState.update { it.copy(wifiOnlyStreaming = enabled) }
    }

    fun setAutoplayPreviews(enabled: Boolean) {
        prefs?.edit()?.putBoolean("autoplay_previews", enabled)?.apply()
        _uiState.update { it.copy(autoplayPreviews = enabled) }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        prefs?.edit()?.putBoolean("wifi_only_downloads", enabled)?.apply()
        _uiState.update { it.copy(wifiOnlyDownloads = enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("notifications_enabled", enabled)?.apply()
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun clearCache() {
        prefs?.edit()?.putString("cache_size", "0 B")?.apply()
        _uiState.update { it.copy(cacheSize = "0 B") }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = ProfileUiState(isLoading = false)
        }
    }

    private fun formatExpiry(subscription: Subscription?): String? {
        if (subscription == null) return null
        val timeMs = subscription.currentPeriodEnd
            ?: subscription.expiryDate?.toDate()?.time
            ?: return null

        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(Date(timeMs))
    }
}
