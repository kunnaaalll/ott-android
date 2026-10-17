package com.ott.platform.data.model

/**
 * Canonical subscription tiers for Chumbak OTT.
 * Synchronized with Website pricing, backend Cashfree order verification, and Habbit Plus specifications.
 */
object PlanDefaults {

    fun getPlans(tenantId: String = "default-tenant"): List<Plan> = listOf(
        Plan(
            id = "plan_year_vip",
            tenantId = tenantId,
            name = "Limited Time Offer for Limited Customer",
            description = "Exclusive annual VIP access to all blockbusters & originals for an entire year",
            price = 599.0,
            currency = "INR",
            billingPeriod = "365 Days",
            badge = "365 Days",
            durationDays = 365,
            featured = true,
            active = true,
            features = listOf(
                "Unlimited Streaming",
                "Watch Content In HD, 4K, 2K",
                "Watch Everywhere",
                "New Release In Every Week"
            )
        ),
        Plan(
            id = "plan_six_months",
            tenantId = tenantId,
            name = "Six Month",
            description = "Half-year uninterrupted streaming pass for cinema lovers",
            price = 449.0,
            currency = "INR",
            billingPeriod = "180 Days",
            badge = "180 Days",
            durationDays = 180,
            featured = false,
            active = true,
            features = listOf(
                "Unlimited Streaming",
                "Watch Content In HD, 4K, 2K",
                "Watch Everywhere",
                "New Release In Every Week"
            )
        ),
        Plan(
            id = "plan_three_months",
            tenantId = tenantId,
            name = "Three Month",
            description = "Quarterly pass packed with popular originals and latest releases",
            price = 349.0,
            currency = "INR",
            billingPeriod = "90 Days",
            badge = "90 Days",
            durationDays = 90,
            featured = false,
            active = true,
            features = listOf(
                "Unlimited Streaming",
                "Watch Content In HD, 4K, 2K",
                "Watch Everywhere",
                "New Release In Every Week"
            )
        ),
        Plan(
            id = "plan_one_month",
            tenantId = tenantId,
            name = "1 Month",
            description = "Standard monthly membership for regular streaming",
            price = 279.0,
            currency = "INR",
            billingPeriod = "30 Days",
            badge = "30 Days",
            durationDays = 30,
            featured = false,
            active = true,
            features = listOf(
                "Unlimited Streaming",
                "Watch Content In HD, 4K, 2K",
                "Watch Everywhere",
                "New Release In Every Week"
            )
        ),
        Plan(
            id = "plan_trial_package",
            tenantId = tenantId,
            name = "Trial Package",
            description = "Short-term access to preview full movies and binge web series",
            price = 199.0,
            currency = "INR",
            billingPeriod = "7 Days",
            badge = "7 Days",
            durationDays = 7,
            featured = false,
            active = true,
            features = listOf(
                "Unlimited Streaming",
                "Watch Content In HD, 4K, 2K",
                "Watch Everywhere",
                "New Release In Every Week"
            )
        ),
        Plan(
            id = "plan_test_package",
            tenantId = tenantId,
            name = "TEST",
            description = "Instant 1-rupee test package for payment gateway validation",
            price = 1.0,
            currency = "INR",
            billingPeriod = "7 Days",
            badge = "7 Days",
            durationDays = 7,
            featured = false,
            active = true,
            features = listOf(
                "Unlimited Streaming",
                "Watch Content In HD, 4K, 2K",
                "Watch Everywhere"
            )
        )
    )
}
