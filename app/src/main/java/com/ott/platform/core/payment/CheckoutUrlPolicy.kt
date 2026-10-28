package com.ott.platform.core.payment

import java.net.URI

/**
 * Decides which URLs the app may open in the browser as a checkout handoff.
 *
 * The backend returns the URL, but the app does not trust it blindly: a compromised response or
 * network path must not be able to send a signed-in user to an arbitrary site. Only the exact
 * https handoff landing page on the Chumbak website is accepted.
 */
object CheckoutUrlPolicy {

    const val TRUSTED_HOST = "thechumbak.com"
    const val HANDOFF_PATH = "/checkout/handoff"

    fun isTrustedHandoffUrl(url: String?, trustedHost: String = TRUSTED_HOST): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return false
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        if (!uri.host.equals(trustedHost, ignoreCase = true)) return false
        if (uri.path != HANDOFF_PATH) return false
        // The one-time token lives in the fragment only, so it never reaches servers or logs.
        if (uri.rawQuery != null) return false
        val fragment = uri.rawFragment ?: return false
        return TOKEN_FRAGMENT.matches(fragment)
    }

    /** Fragment form produced by the backend: `t=<43 base64url characters>`. */
    private val TOKEN_FRAGMENT = Regex("^t=[A-Za-z0-9_-]{43}$")

    /** Removes the one-time token so a URL can be logged or shown safely. */
    fun redact(url: String): String = url.substringBefore('#')
}
