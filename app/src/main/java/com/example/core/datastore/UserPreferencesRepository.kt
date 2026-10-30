package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "musabbih_prefs")

data class UserPreferences(
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val useArabicIndicNumerals: Boolean = true,
    val hapticEnabled: Boolean = true,
    val morningReminderEnabled: Boolean = false,
    val morningReminderHour: Int = 6,
    val morningReminderMinute: Int = 0,
    val eveningReminderEnabled: Boolean = false,
    val eveningReminderHour: Int = 18,
    val eveningReminderMinute: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_ARABIC_INDIC = booleanPreferencesKey("use_arabic_indic")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val MORNING_REMINDER_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
        val MORNING_REMINDER_HOUR = intPreferencesKey("morning_reminder_hour")
        val MORNING_REMINDER_MINUTE = intPreferencesKey("morning_reminder_minute")
        val EVENING_REMINDER_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
        val EVENING_REMINDER_HOUR = intPreferencesKey("evening_reminder_hour")
        val EVENING_REMINDER_MINUTE = intPreferencesKey("evening_reminder_minute")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
                useArabicIndicNumerals = preferences[PreferencesKeys.USE_ARABIC_INDIC] ?: true,
                hapticEnabled = preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true,
                morningReminderEnabled = preferences[PreferencesKeys.MORNING_REMINDER_ENABLED] ?: false,
                morningReminderHour = preferences[PreferencesKeys.MORNING_REMINDER_HOUR] ?: 6,
                morningReminderMinute = preferences[PreferencesKeys.MORNING_REMINDER_MINUTE] ?: 0,
                eveningReminderEnabled = preferences[PreferencesKeys.EVENING_REMINDER_ENABLED] ?: false,
                eveningReminderHour = preferences[PreferencesKeys.EVENING_REMINDER_HOUR] ?: 18,
                eveningReminderMinute = preferences[PreferencesKeys.EVENING_REMINDER_MINUTE] ?: 0
            )
        }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setUseArabicIndicNumerals(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_ARABIC_INDIC] = enabled
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun setMorningReminder(enabled: Boolean, hour: Int = 6, minute: Int = 0) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MORNING_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.MORNING_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.MORNING_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setEveningReminder(enabled: Boolean, hour: Int = 18, minute: Int = 0) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EVENING_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.EVENING_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.EVENING_REMINDER_MINUTE] = minute
        }
    }
}
