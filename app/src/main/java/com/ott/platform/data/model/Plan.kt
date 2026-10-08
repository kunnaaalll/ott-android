package com.ott.platform.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore: plans/{planId}
 *
 * Subscription plans offered to users.
 * Example: Monthly ₹199, Yearly ₹1999.
 */
data class Plan(
    @DocumentId
    val id: String = "",

    @PropertyName("tenantId")
    val tenantId: String = "",

    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "INR",

    @PropertyName("billingPeriod")
    val billingPeriod: String = "", // "monthly", "yearly"

    val active: Boolean = true,
) {
    /**
     * Returns a formatted price string like "₹199 / Month"
     */
    fun formattedPrice(): String {
        val symbol = when (currency.uppercase()) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            else -> currency
        }
        val period = when (billingPeriod.lowercase()) {
            "monthly" -> "Month"
            "yearly" -> "Year"
            else -> billingPeriod
        }
        val amount = if (price == price.toLong().toDouble()) {
            price.toLong().toString()
        } else {
            String.format("%.2f", price)
        }
        return "$symbol$amount / $period"
    }
}
