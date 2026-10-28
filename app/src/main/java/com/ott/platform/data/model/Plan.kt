package com.ott.platform.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Locale

/**
 * Firestore: plans/{planId}
 *
 * Subscription plans offered to users.
 * Example: Monthly ₹199, Yearly ₹1999, 365 Days ₹599.
 */
data class Plan(
    @DocumentId
    val id: String = "",

    @PropertyName("tenantId")
    val tenantId: String = "",

    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String? = "INR",

    @PropertyName("billingPeriod")
    val billingPeriod: String? = "", // "monthly", "yearly", "365 Days"

    val active: Boolean = true,

    val badge: String? = "", // e.g. "365 Days", "180 Days", "90 Days", "30 Days", "7 Days"
    val durationDays: Int = 30,
    val featured: Boolean = false,
    val features: List<String>? = emptyList()
) {
    /**
     * Returns a formatted currency amount string like "₹599" or "₹1"
     */
    fun formattedAmount(): String {
        val curr = currency?.takeIf { it.isNotBlank() } ?: "INR"
        val symbol = when (curr.uppercase(Locale.US)) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            else -> curr
        }
        val safePrice = if (price.isNaN()) 0.0 else price
        val amount = if (safePrice == safePrice.toLong().toDouble()) {
            safePrice.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", safePrice)
        }
        return "$symbol$amount"
    }

    /**
     * Returns the duration period formatted for badges/display like "365 Days"
     */
    fun formattedPeriod(): String {
        if (!badge.isNullOrBlank()) return badge
        val period = billingPeriod?.takeIf { it.isNotBlank() } ?: ""
        return when (period.lowercase(Locale.US)) {
            "monthly" -> "30 Days"
            "yearly" -> "365 Days"
            else -> period.ifBlank { "$durationDays Days" }
        }
    }

    /**
     * Returns a formatted price string like "₹199 / Month" or "₹599 / 365 Days"
     */
    fun formattedPrice(): String {
        val curr = currency?.takeIf { it.isNotBlank() } ?: "INR"
        val symbol = when (curr.uppercase(Locale.US)) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            else -> curr
        }
        val period = when ((billingPeriod ?: "").lowercase(Locale.US)) {
            "monthly" -> "Month"
            "yearly" -> "Year"
            else -> formattedPeriod()
        }
        val safePrice = if (price.isNaN()) 0.0 else price
        val amount = if (safePrice == safePrice.toLong().toDouble()) {
            safePrice.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", safePrice)
        }
        return if (period.isNotBlank()) "$symbol$amount / $period" else "$symbol$amount"
    }
}


