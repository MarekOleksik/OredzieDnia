package com.example.oredziednia

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OredzieFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val name = data[EXTRA_APPARITION_NAME]?.takeIf { it.isNotBlank() } ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_APPARITION_ID, data[EXTRA_APPARITION_ID]?.toIntOrNull() ?: -1)
            putExtra(EXTRA_APPARITION_NAME, name)
            putExtra(EXTRA_APPARITION_LOCATION, data[EXTRA_APPARITION_LOCATION] ?: "")
            putExtra(EXTRA_APPARITION_MESSAGE, data[EXTRA_APPARITION_MESSAGE] ?: "")
            putExtra(EXTRA_APPARITION_DATE, data[EXTRA_APPARITION_DATE] ?: "")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, FCM_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(R.string.notification_channel_description) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val messageText = data[EXTRA_APPARITION_MESSAGE] ?: ""
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(name)
            .setContentText(messageText.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText.take(400)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(FCM_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TOPIC = "new_apparitions"
        private const val FCM_NOTIFICATION_ID = 2
    }
}
