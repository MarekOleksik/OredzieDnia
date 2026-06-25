package com.example.oredziednia

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

const val NOTIFICATION_CHANNEL_ID = "daily_apparition"
private const val NOTIFICATION_ID = 1

const val EXTRA_APPARITION_ID = "apparition_id"
const val EXTRA_APPARITION_NAME = "apparition_name"
const val EXTRA_APPARITION_LOCATION = "apparition_location"
const val EXTRA_APPARITION_MESSAGE = "apparition_message"
const val EXTRA_APPARITION_DATE = "apparition_date"

fun Intent.apparitionExtra(): Apparition? {
    val name = getStringExtra(EXTRA_APPARITION_NAME) ?: return null
    val id = getIntExtra(EXTRA_APPARITION_ID, -1)
    return Apparition(
        id = if (id == -1) null else id,
        name = name,
        location = getStringExtra(EXTRA_APPARITION_LOCATION) ?: "",
        message = getStringExtra(EXTRA_APPARITION_MESSAGE) ?: "",
        date = getStringExtra(EXTRA_APPARITION_DATE) ?: ""
    )
}

class DailyNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val apparitions = try {
            SupabaseApparitionRepository().getAll().sortedBy { it.date }
        } catch (e: Exception) {
            return Result.retry()
        }
        if (apparitions.isEmpty()) return Result.success()

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val apparition = apparitions[dayOfYear % apparitions.size]

        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_APPARITION_ID, apparition.id ?: -1)
                putExtra(EXTRA_APPARITION_NAME, apparition.name)
                putExtra(EXTRA_APPARITION_LOCATION, apparition.location)
                putExtra(EXTRA_APPARITION_MESSAGE, apparition.message)
                putExtra(EXTRA_APPARITION_DATE, apparition.date)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notification_channel_description) }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(apparition.name)
            .setContentText(apparition.message.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(apparition.message.take(400)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)

        return Result.success()
    }
}
