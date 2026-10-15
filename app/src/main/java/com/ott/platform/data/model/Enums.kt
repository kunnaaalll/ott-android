package com.ott.platform.data.model

/**
 * Subscription lifecycle states.
 * Managed exclusively by trusted Cloud Functions — never written by the client.
 */
enum class SubscriptionStatus {
    PENDING,
    ACTIVE,
    GRACE_PERIOD,
    PAUSED,
    CANCELLED,
    EXPIRED,
    FAILED,
    REFUNDED,
    ;

    companion object {
        fun fromString(value: String?): SubscriptionStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: EXPIRED
        }
    }
}

/**
 * Content publication status.
 */
enum class ContentStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
    ;

    companion object {
        fun fromString(value: String?): ContentStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DRAFT
        }
    }
}
