package com.hobbeast.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.local.UserPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesStore,
) : ViewModel() {

    val isDarkTheme         = prefs.isDarkTheme.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val notificationsEnabled = prefs.notificationsEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val reminderHours       = prefs.reminderHours.stateIn(viewModelScope, SharingStarted.Eagerly, 24)

    fun setDarkTheme(v: Boolean)          = viewModelScope.launch { prefs.setDarkTheme(v) }
    fun setNotifications(v: Boolean)      = viewModelScope.launch { prefs.setNotificationsEnabled(v) }
    fun setReminderHours(hours: Int)      = viewModelScope.launch { prefs.setReminderHours(hours) }
}
