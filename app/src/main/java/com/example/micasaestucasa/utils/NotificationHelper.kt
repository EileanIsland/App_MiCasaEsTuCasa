package com.example.micasaestucasa.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.micasaestucasa.MainActivity

object NotificationHelper {
    private const val CHANNEL_CHAT_ID = "it.uniupo.micasa.chat"
    private const val CHANNEL_CHAT_NAME = "Messaggi Chat"
    private const val CHANNEL_CHAT_DESCRIPTION = "Notifiche per i nuovi messaggi"

    private const val CHANNEL_BOOKING_ID = "it.uniupo.micasa.booking"
    private const val CHANNEL_BOOKING_NAME = "Nuove prenotazioni"
    private const val CHANNEL_BOOKING_DESCRIPTION = "Notifiche per le nuove prenotazioni"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val chatChannel = NotificationChannel(
                CHANNEL_CHAT_ID,
                CHANNEL_CHAT_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_CHAT_DESCRIPTION
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }

            val bookingChannel = NotificationChannel(
                CHANNEL_BOOKING_ID,
                CHANNEL_BOOKING_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_BOOKING_DESCRIPTION
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(bookingChannel)
        }
    }

    fun sendChatNotification(context: Context, title: String, message: String, chatId: String) {
        sendNotification(context, title, message, CHANNEL_CHAT_ID, 101, "CHAT", chatId)
    }

    fun sendBookingNotification(context: Context, title: String, message: String) {
        sendNotification(context, title, message, CHANNEL_BOOKING_ID, 102, "BOOKING")
    }


    private fun sendNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String,
        notificationId: Int,
        type: String,
        chatId: String? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            if (chatId != null) putExtra("chatId", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (notificationId + System.currentTimeMillis().toInt()),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = Notification.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}




