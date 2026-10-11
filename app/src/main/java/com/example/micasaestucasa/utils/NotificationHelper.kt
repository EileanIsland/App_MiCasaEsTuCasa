package com.example.micasaestucasa.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager


import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.micasaestucasa.MainActivity
import com.example.micasaestucasa.R

object NotificationHelper {
    private const val CHANNEL_ID_CHAT = "chat_messages"
    private const val CHANNEL_ID_BOOKING = "booking_alerts"

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        type: String,
        chatId: String? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (type == "CHAT") "Messaggi Chat" else "Prenotazioni"
            val importance = NotificationManager.IMPORTANCE_HIGH // Fa il "Pop" (Heads-up)
            val channel = NotificationChannel(
                if (type == "CHAT") CHANNEL_ID_CHAT else CHANNEL_ID_BOOKING,
                name,
                importance
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
            putExtra("chatId", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(context, if (type == "CHAT") CHANNEL_ID_CHAT else CHANNEL_ID_BOOKING)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}