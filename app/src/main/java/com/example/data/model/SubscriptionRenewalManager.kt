package com.example.data.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Calendar

data class RenewalStatus(
    val providerId: String,
    val providerName: String,
    val renewalDayOfMonth: Int,
    val daysUntilRenewal: Int,
    val isImminent: Boolean, // True if renewal is within 3 days (72 hours)
    val cancelUrl: String
)

object SubscriptionRenewalManager {

    private val DIRECT_CANCEL_URLS = mapOf(
        "netflix" to "https://www.netflix.com/cancelplan",
        "max" to "https://auth.max.com/subscription",
        "disney" to "https://www.disneyplus.com/account/subscription",
        "hulu" to "https://secure.hulu.com/account",
        "paramount" to "https://www.paramountplus.com/account/",
        "criterion" to "https://www.criterionchannel.com/settings/manage-subscription",
        "apple" to "https://apple.co/2Th4vqI",
        "prime" to "https://www.amazon.com/gp/video/subscriptions/manage",
        "peacock" to "https://www.peacocktv.com/account/plans",
        "mubi" to "https://mubi.com/settings",
        "shudder" to "https://www.shudder.com/member/account",
        "britbox" to "https://www.britbox.com/account"
    )

    fun getDirectCancelUrl(providerId: String): String {
        val key = providerId.lowercase().trim()
        return DIRECT_CANCEL_URLS[key] 
            ?: DIRECT_CANCEL_URLS.entries.firstOrNull { key.contains(it.key) }?.value
            ?: "https://www.google.com/search?q=how+to+cancel+$key+subscription"
    }

    /**
     * Helper to get integer days until renewal, or null if inactive/unspecified.
     */
    fun getDaysUntilRenewal(provider: StreamingProvider): Int? {
        if (!provider.isActive) return null
        return getRenewalStatus(provider).daysUntilRenewal
    }

    /**
     * Calculates the days remaining until the next billing date for a provider.
     * Uses subscriptionStartDate to determine billing day of month (defaults to 1st if unset).
     */
    fun getRenewalStatus(provider: StreamingProvider): RenewalStatus {
        val now = Calendar.getInstance()
        val billingDay = if (provider.subscriptionStartDate != null && provider.subscriptionStartDate > 0) {
            val startCal = Calendar.getInstance().apply { timeInMillis = provider.subscriptionStartDate }
            startCal.get(Calendar.DAY_OF_MONTH)
        } else {
            1 // Default to 1st of month
        }

        val nextRenewal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, billingDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today is past the renewal day this month, next renewal is next month
        if (now.after(nextRenewal)) {
            nextRenewal.add(Calendar.MONTH, 1)
        }

        val diffMillis = nextRenewal.timeInMillis - now.timeInMillis
        val daysUntil = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        return RenewalStatus(
            providerId = provider.id,
            providerName = provider.name,
            renewalDayOfMonth = billingDay,
            daysUntilRenewal = daysUntil,
            isImminent = daysUntil in 0..3,
            cancelUrl = getDirectCancelUrl(provider.id)
        )
    }

    /**
     * Opens the official direct-to-cancellation webpage in browser/Chrome Custom Tabs.
     */
    fun openCancellationPage(context: Context, providerId: String) {
        val url = getDirectCancelUrl(providerId)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("RenewalManager", "Failed to launch cancel URL: $url", e)
        }
    }
}
