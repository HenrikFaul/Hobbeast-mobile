package com.hobbeast.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.hobbeast.app.data.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderReschedulerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val activeReminders = reminderRepository.getActiveReminders().first()
        val workManager = WorkManager.getInstance(applicationContext)
        val now = System.currentTimeMillis()

        activeReminders.forEach { reminder ->
            val delayMs = reminder.triggerAtMillis - now
            if (delayMs > 0) {
                val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setId(UUID.fromString(reminder.workRequestId ?: UUID.randomUUID().toString()))
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(androidx.work.Data.Builder().putString(ReminderWorker.KEY_EVENT_ID, reminder.eventId.toString()).putString(ReminderWorker.KEY_EVENT_TITLE, reminder.eventTitle.toString()).putInt(ReminderWorker.KEY_HOURS_BEFORE, reminder.type.replace("h", "").toIntOrNull() ?: 24).build())
                    .addTag("reminder_${reminder.eventId}")
                    .build()
                workManager.enqueue(workRequest)
            }
        }
        return Result.success()
    }
}

