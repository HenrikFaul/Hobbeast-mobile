package com.hobbeast.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hobbeast.app.MainActivity
import com.hobbeast.app.worker.ReminderWorker

/**
 * Handles incoming FCM push messages.
 *
 * Expected message data keys:
 *   type        – "reminder" | "event_update" | "message" | "checkin_open"
 *   event_id    – UUID of the related event
 *   title       – Notification title
 *   body        – Notification body text
 *   navigate_to – Optional deep link path, e.g. "event/abc123"
 */
class HobbeastMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: send token to Supabase profiles table via Edge Function
        // supabase.from("profiles").update { set("push_token", token) } where id = currentUser
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data        = message.data
        val type        = data["type"] ?: "general"
        val eventId     = data["event_id"]
        val title       = data["title"] ?: message.notification?.title ?: "Hobbeast"
        val body        = data["body"]  ?: message.notification?.body  ?: ""
        val navigateTo  = data["navigate_to"]

        when (type) {
            "reminder"      -> showNotification(title, body, navigateTo, CHANNEL_REMINDERS, eventId.hashCode())
            "event_update"  -> showNotification(title, body, navigateTo, CHANNEL_UPDATES, eventId.hashCode())
            "message"       -> showNotification(title, body, navigateTo, CHANNEL_MESSAGES, eventId.hashCode())
            "checkin_open"  -> showNotification(title, body, navigateTo, CHANNEL_REMINDERS, eventId.hashCode())
            else            -> showNotification(title, body, navigateTo, CHANNEL_GENERAL, title.hashCode())
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        navigateTo: String?,
        channelId: String,
        notificationId: Int,
    ) {
        createChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            navigateTo?.let { putExtra("navigate_to", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        listOf(
            Triple(CHANNEL_REMINDERS, "Esemény emlékeztetők",   NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_UPDATES,   "Esemény frissítések",    NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_MESSAGES,  "Üzenetek",               NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_GENERAL,   "Általános értesítések",  NotificationManager.IMPORTANCE_LOW),
        ).forEach { (id, name, importance) ->
            nm.createNotificationChannel(NotificationChannel(id, name, importance))
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "hobbeast_reminders"
        const val CHANNEL_UPDATES   = "hobbeast_updates"
        const val CHANNEL_MESSAGES  = "hobbeast_messages"
        const val CHANNEL_GENERAL   = "hobbeast_general"
    }
}
