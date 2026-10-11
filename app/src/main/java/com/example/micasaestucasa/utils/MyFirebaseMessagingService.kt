package com.example.micasaestucasa.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.micasaestucasa.MainActivity
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.repository.UsersRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val type = data["type"] ?: "GENERIC"
        val title = data["title"] ?: remoteMessage.notification?.title ?: "miCasaEsTuCasa"
        val body = data["body"] ?: remoteMessage.notification?.body ?: ""

        val chatId = data["chatId"]
        val bookingId = data["bookingId"]

        showNotification(title, body, type, chatId, bookingId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        UsersRepository.getCurrentUid()?.let { uid ->
            scope.launch {
                UsersRepository.updateFcmToken(uid, token)
            }
        }
    }

    private fun showNotification(
        title: String,
        message: String,
        type: String,
        chatId: String?,
        bookingId: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = when (type) {
            "CHAT" -> "chat_channel"
            "BOOKING" -> "booking_channel"
            else -> "default_channel"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (type == "CHAT") "Messaggi Chat" else "Prenotazioni"
            val importance = if (type == "CHAT") NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "Notifiche relative a $name"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("notification_type", type)
            putExtra("chatId", chatId)
            putExtra("bookingId", bookingId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}


