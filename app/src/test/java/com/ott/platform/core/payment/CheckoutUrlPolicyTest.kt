package com.ott.platform.core.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutUrlPolicyTest {

    private val token = "A".repeat(43)
    private val good = "https://thechumbak.com/checkout/handoff#t=$token"

    @Test
    fun `accepts the exact https handoff url with the token in the fragment`() {
        assertTrue(CheckoutUrlPolicy.isTrustedHandoffUrl(good))
        assertTrue(CheckoutUrlPolicy.isTrustedHandoffUrl("https://THECHUMBAK.com/checkout/handoff#t=$token"))
        assertTrue(CheckoutUrlPolicy.isTrustedHandoffUrl("https://thechumbak.com:443/checkout/handoff#t=$token"))
    }

    @Test
    fun `rejects other hosts, look-alike hosts and userinfo tricks`() {
        listOf(
            "https://evil.example/checkout/handoff#t=$token",
            "https://thechumbak.com.evil.example/checkout/handoff#t=$token",
            "https://evilthechumbak.com/checkout/handoff#t=$token",
            "https://thechumbak.com@evil.example/checkout/handoff#t=$token",
            "https://user:pw@thechumbak.com/checkout/handoff#t=$token",
            "https://thechumbak.com:8443/checkout/handoff#t=$token"
        ).forEach { assertFalse(it, CheckoutUrlPolicy.isTrustedHandoffUrl(it)) }
    }

    @Test
    fun `rejects non-https schemes`() {
        listOf(
            "http://thechumbak.com/checkout/handoff#t=$token",
            "intent://thechumbak.com/checkout/handoff#t=$token",
            "javascript:alert(1)",
            "chumbak://subscription/status",
            "file:///sdcard/x"
        ).forEach { assertFalse(it, CheckoutUrlPolicy.isTrustedHandoffUrl(it)) }
    }

    @Test
    fun `rejects other paths and tokens placed in the query string`() {
        listOf(
            "https://thechumbak.com/pricing#t=$token",
            "https://thechumbak.com/checkout/handoff/extra#t=$token",
            "https://thechumbak.com/checkout/handoff?t=$token",
            "https://thechumbak.com/checkout/handoff?next=x#t=$token",
            "https://thechumbak.com/checkout/../admin#t=$token"
        ).forEach { assertFalse(it, CheckoutUrlPolicy.isTrustedHandoffUrl(it)) }
    }

    @Test
    fun `rejects missing, short, long or malformed tokens`() {
        listOf(
            "https://thechumbak.com/checkout/handoff",
            "https://thechumbak.com/checkout/handoff#",
            "https://thechumbak.com/checkout/handoff#t=",
            "https://thechumbak.com/checkout/handoff#t=${"A".repeat(42)}",
            "https://thechumbak.com/checkout/handoff#t=${"A".repeat(44)}",
            "https://thechumbak.com/checkout/handoff#t=${"A".repeat(42)}!",
            "https://thechumbak.com/checkout/handoff#x=$token",
            "https://thechumbak.com/checkout/handoff#t=$token&redirect=https://evil.example"
        ).forEach { assertFalse(it, CheckoutUrlPolicy.isTrustedHandoffUrl(it)) }
    }

    @Test
    fun `rejects null, blank and unparsable input`() {
        assertFalse(CheckoutUrlPolicy.isTrustedHandoffUrl(null))
        assertFalse(CheckoutUrlPolicy.isTrustedHandoffUrl(""))
        assertFalse(CheckoutUrlPolicy.isTrustedHandoffUrl("   "))
        assertFalse(CheckoutUrlPolicy.isTrustedHandoffUrl("https://thechumbak.com/ checkout/handoff#t=$token"))
        assertFalse(CheckoutUrlPolicy.isTrustedHandoffUrl("not a url"))
    }

    @Test
    fun `redact removes the one-time token`() {
        assertEquals("https://thechumbak.com/checkout/handoff", CheckoutUrlPolicy.redact(good))
        assertFalse(CheckoutUrlPolicy.redact(good).contains(token))
    }
}
