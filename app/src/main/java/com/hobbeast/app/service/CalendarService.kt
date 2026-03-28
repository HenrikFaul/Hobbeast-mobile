package com.hobbeast.app.service

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Opens the system calendar app with the event pre-filled (no permission needed).
     * Uses the Calendar Intent API – works on all Android versions.
     */
    fun openAddToCalendarIntent(
        title: String,
        description: String,
        location: String,
        startTimeIso: String,
        endTimeIso: String? = null,
    ): Intent {
        val startMillis = runCatching { Instant.parse(startTimeIso).toEpochMilli() }.getOrDefault(0L)
        val endMillis = endTimeIso?.let {
            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
        } ?: (startMillis + 2 * 60 * 60 * 1000L) // default 2h duration

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Insert event directly using the ContentProvider (requires READ/WRITE_CALENDAR permission).
     * Returns the inserted event ID, or null on failure.
     */
    fun insertCalendarEvent(
        title: String,
        description: String,
        location: String,
        startTimeIso: String,
        endTimeIso: String? = null,
        calendarId: Long = 1L,
        timeZone: String = java.util.TimeZone.getDefault().id,
    ): Long? {
        val startMillis = runCatching { Instant.parse(startTimeIso).toEpochMilli() }.getOrNull() ?: return null
        val endMillis   = endTimeIso?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: (startMillis + 2 * 60 * 60 * 1000L)

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
        }

        return try {
            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.let { ContentUris.parseId(it) }
        } catch (e: SecurityException) {
            null   // Permission not granted – caller should use Intent API instead
        }
    }

    /** Add a reminder alarm for an existing calendar event. */
    fun addCalendarReminder(eventId: Long, minutesBefore: Int = 60) {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            put(CalendarContract.Reminders.MINUTES, minutesBefore)
        }
        try {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        } catch (_: SecurityException) {}
    }
}
