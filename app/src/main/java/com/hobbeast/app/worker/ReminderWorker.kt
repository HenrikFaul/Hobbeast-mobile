package com.hobbeast.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hobbeast.app.MainActivity
import com.hobbeast.app.R
import com.hobbeast.app.data.local.ReminderDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderDao: ReminderDao,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_EVENT_ID    = "event_id"
        const val KEY_EVENT_TITLE = "event_title"
        const val KEY_HOURS_BEFORE = "hours_before"

        const val CHANNEL_ID = "hobbeast_reminders"
        const val CHANNEL_NAME = "Esemény emlékeztetők"
    }

    override suspend fun doWork(): Result {
        val eventId    = inputData.getString(KEY_EVENT_ID)    ?: return Result.failure()
        val eventTitle = inputData.getString(KEY_EVENT_TITLE) ?: return Result.failure()
        val hoursBefore = inputData.getInt(KEY_HOURS_BEFORE, 24)

        createNotificationChannel()
        showNotification(eventId, eventTitle, hoursBefore)

        // Mark reminder as fired
        reminderDao.deactivateReminder("$eventId-${hoursBefore}h")

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Értesítések a közelgő eseményekről"
                enableVibration(true)
            }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showNotification(eventId: String, eventTitle: String, hoursBefore: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "event/$eventId")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = when (hoursBefore) {
            1    -> "Az esemény 1 óra múlva kezdődik!"
            24   -> "Holnap kezdődik: $eventTitle"
            else -> "$hoursBefore óra múlva kezdődik: $eventTitle"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎉 $eventTitle")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(eventId.hashCode(), notification)
        }
    }
}
