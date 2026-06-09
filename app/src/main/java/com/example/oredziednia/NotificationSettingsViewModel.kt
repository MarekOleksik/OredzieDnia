package com.example.oredziednia

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

private val Context.notifPrefs: DataStore<Preferences> by preferencesDataStore("notif_settings")

private val KEY_ENABLED = booleanPreferencesKey("enabled")
private val KEY_HOUR = intPreferencesKey("hour")
private val KEY_MINUTE = intPreferencesKey("minute")

data class NotificationSettings(
    val enabled: Boolean = true,
    val hour: Int = 8,
    val minute: Int = 0
)

class NotificationSettingsViewModel(private val app: Application) : AndroidViewModel(app) {

    val settings: StateFlow<NotificationSettings> = app.notifPrefs.data
        .map { prefs ->
            NotificationSettings(
                enabled = prefs[KEY_ENABLED] ?: true,
                hour    = prefs[KEY_HOUR]    ?: 8,
                minute  = prefs[KEY_MINUTE]  ?: 0
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationSettings())

    fun scheduleIfNeeded() {
        viewModelScope.launch {
            val s = settings.value
            if (s.enabled) schedule(s.hour, s.minute, ExistingPeriodicWorkPolicy.KEEP)
        }
    }

    fun save(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            app.notifPrefs.edit { prefs ->
                prefs[KEY_ENABLED] = enabled
                prefs[KEY_HOUR]    = hour
                prefs[KEY_MINUTE]  = minute
            }
            if (enabled) {
                schedule(hour, minute, ExistingPeriodicWorkPolicy.REPLACE)
            } else {
                WorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
            }
        }
    }

    private fun schedule(hour: Int, minute: Int, policy: ExistingPeriodicWorkPolicy) {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val request = PeriodicWorkRequestBuilder<DailyNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
    }

    companion object {
        const val WORK_NAME = "daily_apparition"
    }
}
