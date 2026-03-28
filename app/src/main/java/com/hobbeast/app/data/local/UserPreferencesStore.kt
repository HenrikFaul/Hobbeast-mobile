package com.hobbeast.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hobbeast_prefs")

@Singleton
class UserPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val KEY_THEME_DARK      = booleanPreferencesKey("theme_dark")
        val KEY_LAST_FILTER     = stringPreferencesKey("last_filter_mode")
        val KEY_NOTIFICATIONS   = booleanPreferencesKey("notifications_enabled")
        val KEY_REMINDER_HOURS  = intPreferencesKey("reminder_hours_before")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_USER_ID         = stringPreferencesKey("user_id")
        val KEY_LAST_LAT        = doublePreferencesKey("last_lat")
        val KEY_LAST_LON        = doublePreferencesKey("last_lon")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_THEME_DARK] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }
    val reminderHours: Flow<Int> = context.dataStore.data.map { it[KEY_REMINDER_HOURS] ?: 24 }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val lastLocation: Flow<Pair<Double, Double>?> = context.dataStore.data.map { prefs ->
        val lat = prefs[KEY_LAST_LAT] ?: return@map null
        val lon = prefs[KEY_LAST_LON] ?: return@map null
        Pair(lat, lon)
    }

    suspend fun setDarkTheme(enabled: Boolean) = context.dataStore.edit { it[KEY_THEME_DARK] = enabled }
    suspend fun setNotificationsEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    suspend fun setReminderHours(hours: Int) = context.dataStore.edit { it[KEY_REMINDER_HOURS] = hours }
    suspend fun setOnboardingDone() = context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    suspend fun setUserId(id: String) = context.dataStore.edit { it[KEY_USER_ID] = id }
    suspend fun setLastLocation(lat: Double, lon: Double) = context.dataStore.edit {
        it[KEY_LAST_LAT] = lat
        it[KEY_LAST_LON] = lon
    }
    suspend fun clear() = context.dataStore.edit { it.clear() }
}
