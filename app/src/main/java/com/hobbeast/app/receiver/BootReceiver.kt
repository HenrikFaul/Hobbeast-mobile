package com.hobbeast.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.hobbeast.app.worker.ReminderReschedulerWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val work = OneTimeWorkRequestBuilder<ReminderReschedulerWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
