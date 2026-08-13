package com.mobdeve.s15.group14.fridyi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Compatibility: handle both old and new extra keys
        val foodName = intent.getStringExtra(EXTRA_FOOD_NAME) ?: intent.getStringExtra("FOOD_NAME") ?: "Food Item"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "fridyi_expiration_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Food Expiration Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for items that are nearing expiration or expired"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Expiring Soon Alert!")
            .setContentText("$foodName is expiring soon! Consume or freeze it.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val EXTRA_FOOD_NAME = "extra_food_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
