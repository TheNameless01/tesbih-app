package com.example.feature.prayer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.core.datastore.dataStore
import com.example.feature.prayer.domain.model.AudioSourceType
import com.example.feature.prayer.domain.model.IndividualAdhanSwitches
import com.example.feature.prayer.domain.model.IndividualAdjustments
import com.example.feature.prayer.domain.model.LocationMode
import com.example.feature.prayer.domain.model.PrayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrayerPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_CONFIGURED = booleanPreferencesKey("prayer_is_configured")
        val LOCATION_MODE = stringPreferencesKey("prayer_location_mode")
        val LATITUDE = doublePreferencesKey("prayer_latitude")
        val LONGITUDE = doublePreferencesKey("prayer_longitude")
        val LOCATION_NAME = stringPreferencesKey("prayer_location_name")
        val TIMEZONE = stringPreferencesKey("prayer_timezone")
        val CALCULATION_METHOD = intPreferencesKey("prayer_calc_method")
        val ASR_SCHOOL = intPreferencesKey("prayer_asr_school")
        val GLOBAL_ADJUSTMENT = intPreferencesKey("prayer_global_adjustment")
        val ADJUST_FAJR = intPreferencesKey("prayer_adjust_fajr")
        val ADJUST_SUNRISE = intPreferencesKey("prayer_adjust_sunrise")
        val ADJUST_DHUHR = intPreferencesKey("prayer_adjust_dhuhr")
        val ADJUST_ASR = intPreferencesKey("prayer_adjust_asr")
        val ADJUST_MAGHRIB = intPreferencesKey("prayer_adjust_maghrib")
        val ADJUST_ISHA = intPreferencesKey("prayer_adjust_isha")
        val MASTER_ADHAN_ENABLED = booleanPreferencesKey("prayer_master_adhan_enabled")
        val ADHAN_FAJR = booleanPreferencesKey("prayer_adhan_fajr")
        val ADHAN_DHUHR = booleanPreferencesKey("prayer_adhan_dhuhr")
        val ADHAN_ASR = booleanPreferencesKey("prayer_adhan_asr")
        val ADHAN_MAGHRIB = booleanPreferencesKey("prayer_adhan_maghrib")
        val ADHAN_ISHA = booleanPreferencesKey("prayer_adhan_isha")
        val AUDIO_SOURCE = stringPreferencesKey("prayer_audio_source")
        val CUSTOM_AUDIO_URI = stringPreferencesKey("prayer_custom_audio_uri")
        val CUSTOM_AUDIO_NAME = stringPreferencesKey("prayer_custom_audio_name")
        val PLAYBACK_DURATION = intPreferencesKey("prayer_playback_duration")
        val HOME_CARD_EXPANDED = booleanPreferencesKey("prayer_home_card_expanded")
        val LAST_REFRESH_EPOCH = longPreferencesKey("prayer_last_refresh_epoch")
    }

    val prayerSettingsFlow: Flow<PrayerSettings> = context.dataStore.data.map { prefs ->
        val locModeStr = prefs[Keys.LOCATION_MODE] ?: LocationMode.AUTO.name
        val audioSourceStr = prefs[Keys.AUDIO_SOURCE] ?: AudioSourceType.BUNDLED.name

        PrayerSettings(
            isConfigured = prefs[Keys.IS_CONFIGURED] ?: false,
            locationMode = try { LocationMode.valueOf(locModeStr) } catch (_: Exception) { LocationMode.AUTO },
            latitude = prefs[Keys.LATITUDE] ?: 36.7538, // Default Algiers
            longitude = prefs[Keys.LONGITUDE] ?: 3.0588,
            locationName = prefs[Keys.LOCATION_NAME] ?: "الجزائر العاصمة",
            timezone = prefs[Keys.TIMEZONE] ?: "Africa/Algiers",
            calculationMethodId = prefs[Keys.CALCULATION_METHOD] ?: 19, // Algeria default
            asrSchoolId = prefs[Keys.ASR_SCHOOL] ?: 0,
            globalAdjustmentMinutes = prefs[Keys.GLOBAL_ADJUSTMENT] ?: 0,
            individualAdjustments = IndividualAdjustments(
                fajr = prefs[Keys.ADJUST_FAJR] ?: 0,
                sunrise = prefs[Keys.ADJUST_SUNRISE] ?: 0,
                dhuhr = prefs[Keys.ADJUST_DHUHR] ?: 0,
                asr = prefs[Keys.ADJUST_ASR] ?: 0,
                maghrib = prefs[Keys.ADJUST_MAGHRIB] ?: 0,
                isha = prefs[Keys.ADJUST_ISHA] ?: 0
            ),
            masterAdhanEnabled = prefs[Keys.MASTER_ADHAN_ENABLED] ?: false, // Default OFF
            individualAdhanSwitches = IndividualAdhanSwitches(
                fajr = prefs[Keys.ADHAN_FAJR] ?: true,
                dhuhr = prefs[Keys.ADHAN_DHUHR] ?: true,
                asr = prefs[Keys.ADHAN_ASR] ?: true,
                maghrib = prefs[Keys.ADHAN_MAGHRIB] ?: true,
                isha = prefs[Keys.ADHAN_ISHA] ?: true
            ),
            audioSource = try { AudioSourceType.valueOf(audioSourceStr) } catch (_: Exception) { AudioSourceType.BUNDLED },
            customAudioUri = prefs[Keys.CUSTOM_AUDIO_URI],
            customAudioFileName = prefs[Keys.CUSTOM_AUDIO_NAME],
            playbackDurationSeconds = prefs[Keys.PLAYBACK_DURATION] ?: 30,
            isHomeCardExpanded = prefs[Keys.HOME_CARD_EXPANDED] ?: false,
            lastRefreshEpoch = prefs[Keys.LAST_REFRESH_EPOCH] ?: 0L
        )
    }

    suspend fun setConfigured(configured: Boolean) {
        context.dataStore.edit { it[Keys.IS_CONFIGURED] = configured }
    }

    suspend fun setLocation(
        mode: LocationMode,
        lat: Double,
        lng: Double,
        name: String,
        tz: String
    ) {
        context.dataStore.edit {
            it[Keys.IS_CONFIGURED] = true
            it[Keys.LOCATION_MODE] = mode.name
            it[Keys.LATITUDE] = lat
            it[Keys.LONGITUDE] = lng
            it[Keys.LOCATION_NAME] = name
            it[Keys.TIMEZONE] = tz
        }
    }

    suspend fun setCalculationMethod(methodId: Int) {
        context.dataStore.edit { it[Keys.CALCULATION_METHOD] = methodId }
    }

    suspend fun setAsrSchool(schoolId: Int) {
        context.dataStore.edit { it[Keys.ASR_SCHOOL] = schoolId }
    }

    suspend fun setGlobalAdjustment(minutes: Int) {
        context.dataStore.edit { it[Keys.GLOBAL_ADJUSTMENT] = minutes }
    }

    suspend fun setIndividualAdjustment(
        fajr: Int,
        sunrise: Int,
        dhuhr: Int,
        asr: Int,
        maghrib: Int,
        isha: Int
    ) {
        context.dataStore.edit {
            it[Keys.ADJUST_FAJR] = fajr
            it[Keys.ADJUST_SUNRISE] = sunrise
            it[Keys.ADJUST_DHUHR] = dhuhr
            it[Keys.ADJUST_ASR] = asr
            it[Keys.ADJUST_MAGHRIB] = maghrib
            it[Keys.ADJUST_ISHA] = isha
        }
    }

    suspend fun resetAllAdjustments() {
        context.dataStore.edit {
            it[Keys.GLOBAL_ADJUSTMENT] = 0
            it[Keys.ADJUST_FAJR] = 0
            it[Keys.ADJUST_SUNRISE] = 0
            it[Keys.ADJUST_DHUHR] = 0
            it[Keys.ADJUST_ASR] = 0
            it[Keys.ADJUST_MAGHRIB] = 0
            it[Keys.ADJUST_ISHA] = 0
        }
    }

    suspend fun setMasterAdhanEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MASTER_ADHAN_ENABLED] = enabled }
    }

    suspend fun setIndividualAdhanSwitches(
        fajr: Boolean,
        dhuhr: Boolean,
        asr: Boolean,
        maghrib: Boolean,
        isha: Boolean
    ) {
        context.dataStore.edit {
            it[Keys.ADHAN_FAJR] = fajr
            it[Keys.ADHAN_DHUHR] = dhuhr
            it[Keys.ADHAN_ASR] = asr
            it[Keys.ADHAN_MAGHRIB] = maghrib
            it[Keys.ADHAN_ISHA] = isha
        }
    }

    suspend fun setAudioSource(source: AudioSourceType, uri: String? = null, fileName: String? = null) {
        context.dataStore.edit {
            it[Keys.AUDIO_SOURCE] = source.name
            if (uri != null) it[Keys.CUSTOM_AUDIO_URI] = uri else it.remove(Keys.CUSTOM_AUDIO_URI)
            if (fileName != null) it[Keys.CUSTOM_AUDIO_NAME] = fileName else it.remove(Keys.CUSTOM_AUDIO_NAME)
        }
    }

    suspend fun setPlaybackDuration(seconds: Int) {
        context.dataStore.edit { it[Keys.PLAYBACK_DURATION] = seconds }
    }

    suspend fun setHomeCardExpanded(expanded: Boolean) {
        context.dataStore.edit { it[Keys.HOME_CARD_EXPANDED] = expanded }
    }

    suspend fun setLastRefreshEpoch(epoch: Long) {
        context.dataStore.edit { it[Keys.LAST_REFRESH_EPOCH] = epoch }
    }
}
