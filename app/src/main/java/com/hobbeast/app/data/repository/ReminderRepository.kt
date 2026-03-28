package com.hobbeast.app.data.repository

import android.content.Context
import androidx.work.*
import com.hobbeast.app.data.local.ReminderDao
import com.hobbeast.app.data.local.ReminderEntity
import com.hobbeast.app.worker.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao,
) {
    private val workManager = WorkManager.getInstance(context)

    // ─── Schedule a reminder ──────────────────────────────────────────────────

    suspend fun scheduleReminder(
        eventId: String,
        eventTitle: String,
        eventStartTimeIso: String,
        hoursBeforeEvent: Int = 24,
    ): String {
        val startInstant = Instant.parse(eventStartTimeIso)
        val triggerInstant = startInstant.minus(hoursBeforeEvent.toLong(), ChronoUnit.HOURS)
        val delayMs = triggerInstant.toEpochMilli() - System.currentTimeMillis()

        if (delayMs < 0) return ""  // Event already started

        val workRequestId = UUID.randomUUID().toString()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setId(UUID.fromString(workRequestId))
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_EVENT_ID to eventId,
                    ReminderWorker.KEY_EVENT_TITLE to eventTitle,
                    ReminderWorker.KEY_HOURS_BEFORE to hoursBeforeEvent,
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .addTag("reminder_$eventId")
            .build()

        workManager.enqueue(workRequest)

        val reminderId = "$eventId-${hoursBeforeEvent}h"
        reminderDao.upsertReminder(
            ReminderEntity(
                id = reminderId,
                eventId = eventId,
                eventTitle = eventTitle,
                eventStartTime = eventStartTimeIso,
                triggerAtMillis = triggerInstant.toEpochMilli(),
                type = "${hoursBeforeEvent}h",
                workRequestId = workRequestId,
                isActive = true,
            )
        )
        return reminderId
    }

    // ─── Cancel a reminder ────────────────────────────────────────────────────

    suspend fun cancelReminder(eventId: String) {
        workManager.cancelAllWorkByTag("reminder_$eventId")
        reminderDao.deleteRemindersForEvent(eventId)
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    fun getActiveReminders(): Flow<List<ReminderEntity>> =
        reminderDao.getActiveReminders()

    suspend fun isReminderSet(eventId: String): Boolean =
        reminderDao.getRemindersForEvent(eventId).isNotEmpty()
}
