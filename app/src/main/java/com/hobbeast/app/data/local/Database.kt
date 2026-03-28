package com.hobbeast.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entities ─────────────────────────────────────────────────────────────────

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String?,
    val location: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val organizerId: String,
    val organizerName: String?,
    val imageUrl: String?,
    val category: String,
    val tags: String,            // JSON array as string
    val maxCapacity: Int?,
    val attendeeCount: Int,
    val isPrivate: Boolean,
    val isFree: Boolean,
    val price: Double?,
    val source: String,
    val externalId: String?,
    val externalUrl: String?,
    val isTrending: Boolean,
    val isFeatured: Boolean,
    val isEarlyAccess: Boolean,
    val participationState: String,
    val reminderSet: Boolean,
    val communityPulseScore: Double?,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val bio: String?,
    val avatarUrl: String?,
    val interests: String,   // JSON array
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val distanceKm: Int,
    val locationSharing: Boolean,
    val isOrganizer: Boolean,
    val profileVisibility: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val eventTitle: String,
    val eventStartTime: String,
    val triggerAtMillis: Long,
    val type: String,           // "24h", "1h", "custom"
    val workRequestId: String?,
    val isActive: Boolean = true,
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE isTrending = 1")
    fun getTrendingEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isFeatured = 1")
    fun getFeaturedEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE organizerId = :userId")
    fun getOrganizerEvents(userId: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchEvents(query: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE category = :category")
    fun getEventsByCategory(category: String): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsertEvent(event: EventEntity)

    @Upsert
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: String)

    @Query("DELETE FROM events WHERE cachedAt < :threshold")
    suspend fun deleteStaleCache(threshold: Long)

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int
}

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles WHERE id = :userId")
    suspend fun getProfile(userId: String): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)
}

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY triggerAtMillis ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE eventId = :eventId")
    suspend fun getRemindersForEvent(eventId: String): List<ReminderEntity>

    @Upsert
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isActive = 0 WHERE id = :id")
    suspend fun deactivateReminder(id: String)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteRemindersForEvent(eventId: String)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [EventEntity::class, ProfileEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HobbeastDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun profileDao(): ProfileDao
    abstract fun reminderDao(): ReminderDao
}
