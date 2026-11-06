package com.example.feature.prayer.data.repository

import com.example.core.database.dao.PrayerDao
import com.example.core.database.entity.PrayerTimeEntity
import com.example.feature.prayer.data.api.AlAdhanApiService
import com.example.feature.prayer.domain.model.CalculationMethods
import com.example.feature.prayer.domain.model.DailyPrayerSchedule
import com.example.feature.prayer.domain.model.PrayerSettings
import com.example.feature.prayer.domain.model.PrayerType
import com.example.feature.prayer.domain.model.SinglePrayerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerTimesRepository(
    private val prayerDao: PrayerDao,
    private val preferencesRepository: PrayerPreferencesRepository,
    private val apiService: AlAdhanApiService = AlAdhanApiService.create()
) {

    val prayerSettingsFlow: Flow<PrayerSettings> = preferencesRepository.prayerSettingsFlow

    /**
     * Observes the daily prayer schedule for a specific date, merging Room cache and settings.
     */
    fun observeDailySchedule(date: LocalDate = LocalDate.now()): Flow<DailyPrayerSchedule?> {
        val dateString = date.toString()
        return combine(
            prayerDao.getPrayerTimeFlowForDate(dateString),
            prayerSettingsFlow
        ) { cachedEntity, settings ->
            if (cachedEntity == null) {
                null
            } else {
                mapEntityToDailySchedule(cachedEntity, settings, date)
            }
        }
    }

    suspend fun getDailySchedule(date: LocalDate = LocalDate.now()): DailyPrayerSchedule? {
        val dateString = date.toString()
        val cached = prayerDao.getPrayerTimeForDate(dateString) ?: return null
        val settings = getPrayerSettingsOnce()
        return mapEntityToDailySchedule(cached, settings, date)
    }

    private suspend fun getPrayerSettingsOnce(): PrayerSettings {
        var result = PrayerSettings()
        preferencesRepository.prayerSettingsFlow.collect {
            result = it
            return@collect
        }
        return result
    }

    /**
     * Downloads and caches prayer times for current and next month to guarantee offline availability.
     */
    suspend fun refreshPrayerTimes(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val settings = getPrayerSettingsOnce()
            if (!settings.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Prayer times not yet configured"))
            }

            val now = LocalDate.now()
            val currentYear = now.year
            val currentMonth = now.monthValue

            // Fetch current month
            fetchAndCacheMonth(
                year = currentYear,
                month = currentMonth,
                settings = settings
            )

            // If in the second half of the month, also pre-fetch next month
            if (now.dayOfMonth >= 15) {
                val nextMonthDate = now.plusMonths(1)
                fetchAndCacheMonth(
                    year = nextMonthDate.year,
                    month = nextMonthDate.monthValue,
                    settings = settings
                )
            }

            preferencesRepository.setLastRefreshEpoch(System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchAndCacheMonth(
        year: Int,
        month: Int,
        settings: PrayerSettings
    ) {
        val response = apiService.getMonthlyCalendar(
            year = year,
            month = month,
            latitude = settings.latitude,
            longitude = settings.longitude,
            method = settings.calculationMethodId,
            school = settings.asrSchoolId
        )

        val daysList = response.data
        if (daysList.isNullOrEmpty()) return

        val entities = daysList.mapNotNull { dayData ->
            val timings = dayData.timings ?: return@mapNotNull null
            val gregorian = dayData.date?.gregorian ?: return@mapNotNull null
            val rawDate = gregorian.date ?: return@mapNotNull null // "06-11-2026"

            // Parse DD-MM-YYYY to YYYY-MM-DD
            val parts = rawDate.split("-")
            if (parts.size != 3) return@mapNotNull null
            val dateString = "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"

            val timezone = dayData.meta?.timezone ?: settings.timezone

            PrayerTimeEntity(
                id = "prayer_${dateString}",
                dateString = dateString,
                latitude = settings.latitude,
                longitude = settings.longitude,
                locationName = settings.locationName,
                timezone = timezone,
                fajr = cleanTimeString(timings.fajr),
                sunrise = cleanTimeString(timings.sunrise),
                dhuhr = cleanTimeString(timings.dhuhr),
                asr = cleanTimeString(timings.asr),
                maghrib = cleanTimeString(timings.maghrib),
                isha = cleanTimeString(timings.isha),
                methodId = settings.calculationMethodId,
                schoolId = settings.asrSchoolId,
                updatedAt = System.currentTimeMillis()
            )
        }

        if (entities.isNotEmpty()) {
            prayerDao.insertPrayerTimes(entities)
        }
    }

    fun mapEntityToDailySchedule(
        entity: PrayerTimeEntity,
        settings: PrayerSettings,
        date: LocalDate
    ): DailyPrayerSchedule {
        val zoneId = try {
            ZoneId.of(entity.timezone)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val methodInfo = CalculationMethods.getById(entity.methodId)

        // Raw prayer times
        val rawMap = mapOf(
            PrayerType.FAJR to entity.fajr,
            PrayerType.SUNRISE to entity.sunrise,
            PrayerType.DHUHR to entity.dhuhr,
            PrayerType.ASR to entity.asr,
            PrayerType.MAGHRIB to entity.maghrib,
            PrayerType.ISHA to entity.isha
        )

        val nowLocalDateTime = LocalDateTime.now(zoneId)
        val today = nowLocalDateTime.toLocalDate()

        val items = PrayerType.entries.map { type ->
            val raw = rawMap[type] ?: "00:00"
            val totalOffsetMinutes = settings.globalAdjustmentMinutes + settings.individualAdjustments.forPrayer(type)
            val adjusted = applyOffsetToTime(raw, totalOffsetMinutes)
            val epochMillis = parseTimeToEpochMillis(date, adjusted, zoneId)

            SinglePrayerItem(
                type = type,
                rawTime = raw,
                adjustedTime = adjusted,
                epochMillis = epochMillis,
                isNext = false
            )
        }

        // Determine next upcoming prayer
        // Only prayers (not sunrise) are eligible for "Next Prayer"
        val nowMillis = nowLocalDateTime.atZone(zoneId).toInstant().toEpochMilli()
        val upcomingPrayersToday = items.filter { it.type.isPrayer && it.epochMillis > nowMillis }

        val nextItem = if (upcomingPrayersToday.isNotEmpty()) {
            upcomingPrayersToday.first()
        } else {
            // All prayers for today have passed: next prayer is Fajr tomorrow
            val fajrToday = items.first { it.type == PrayerType.FAJR }
            // Estimated tomorrow Fajr epoch (+ 24h)
            fajrToday.copy(epochMillis = fajrToday.epochMillis + 86400000L)
        }

        val markedItems = items.map { it.copy(isNext = it.type == nextItem.type) }
        val countdown = (nextItem.epochMillis - nowMillis).coerceAtLeast(0L)

        // Stale check (older than 2 days)
        val isStale = (System.currentTimeMillis() - entity.updatedAt) > (2 * 86400000L)

        return DailyPrayerSchedule(
            dateString = entity.dateString,
            locationName = entity.locationName.ifBlank { settings.locationName },
            timezone = entity.timezone,
            methodInfo = methodInfo,
            prayers = markedItems,
            nextPrayer = nextItem,
            countdownMillis = countdown,
            isStale = isStale
        )
    }

    companion object {
        fun cleanTimeString(raw: String?): String {
            if (raw.isNullOrBlank()) return "00:00"
            // Example: "05:32 (CET)" -> "05:32", or "05:32:00" -> "05:32"
            val cleaned = raw.trim().split(" ")[0]
            val parts = cleaned.split(":")
            return if (parts.size >= 2) {
                "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
            } else {
                cleaned
            }
        }

        fun applyOffsetToTime(timeHHmm: String, offsetMinutes: Int): String {
            if (offsetMinutes == 0) return timeHHmm
            val parts = timeHHmm.split(":")
            if (parts.size < 2) return timeHHmm
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0

            var totalMin = (h * 60 + m + offsetMinutes) % 1440
            if (totalMin < 0) totalMin += 1440

            val newH = totalMin / 60
            val newM = totalMin % 60
            return String.format(Locale.US, "%02d:%02d", newH, newM)
        }

        fun parseTimeToEpochMillis(date: LocalDate, timeHHmm: String, zoneId: ZoneId): Long {
            val parts = timeHHmm.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val localDateTime = LocalDateTime.of(date, LocalTime.of(h, m))
            return localDateTime.atZone(zoneId).toInstant().toEpochMilli()
        }
    }
}
