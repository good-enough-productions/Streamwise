package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

object NotificationHelper {
    private const val CHANNEL_ID = "availability_alerts"
    private const val CHANNEL_NAME = "Movie Availability"
    private const val CHANNEL_DESC = "Notifications when a watchlist movie becomes streamable"

    fun showAvailabilityNotification(context: Context, title: String, providerName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle("Now Streaming!")
            .setContentText("\"$title\" is now available on $providerName.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(title.hashCode(), builder.build())
    }

    private const val RENEWAL_CHANNEL_ID = "renewal_alerts"
    private const val RENEWAL_CHANNEL_NAME = "Subscription Renewal Radar"

    fun showRenewalAlert(
        context: Context,
        providerName: String,
        monthlyCost: Double,
        hoursWatched: Double,
        cancelUrl: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(RENEWAL_CHANNEL_ID, RENEWAL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Proactive alerts 48h before underutilized streaming subscriptions auto-renew"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(cancelUrl)).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            providerName.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val costStr = String.format(java.util.Locale.US, "$%.2f", monthlyCost)
        val builder = NotificationCompat.Builder(context, RENEWAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Safe to Pause: $providerName ($costStr/mo)")
            .setContentText("Auto-renews in 48h. Only ${String.format(java.util.Locale.US, "%.1f", hoursWatched)}h watched this cycle. Tap to pause.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Pause Subscription", pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(("renewal_" + providerName).hashCode(), builder.build())
    }
}
