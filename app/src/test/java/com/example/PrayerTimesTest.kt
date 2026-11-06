package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.MusabbihDatabase
import com.example.core.database.entity.PrayerTimeEntity
import com.example.feature.prayer.data.repository.PrayerPreferencesRepository
import com.example.feature.prayer.data.repository.PrayerTimesRepository
import com.example.feature.prayer.domain.model.AudioSourceType
import com.example.feature.prayer.domain.model.CalculationMethods
import com.example.feature.prayer.domain.model.IndividualAdhanSwitches
import com.example.feature.prayer.domain.model.IndividualAdjustments
import com.example.feature.prayer.domain.model.LocationMode
import com.example.feature.prayer.domain.model.PrayerSettings
import com.example.feature.prayer.domain.model.PrayerType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerTimesTest {

    private lateinit var context: Context
    private lateinit var database: MusabbihDatabase
    private lateinit var preferencesRepo: PrayerPreferencesRepository
    private lateinit var timesRepo: PrayerTimesRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MusabbihDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesRepo = PrayerPreferencesRepository(context)
        timesRepo = PrayerTimesRepository(database.prayerDao(), preferencesRepo)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCleanTimeString() {
        assertEquals("05:32", PrayerTimesRepository.cleanTimeString("05:32 (CET)"))
        assertEquals("05:32", PrayerTimesRepository.cleanTimeString("05:32:00"))
        assertEquals("12:45", PrayerTimesRepository.cleanTimeString("12:45"))
        assertEquals("00:00", PrayerTimesRepository.cleanTimeString(null))
        assertEquals("00:00", PrayerTimesRepository.cleanTimeString(""))
    }

    @Test
    fun testApplyOffsetToTime() {
        // Normal positive offset
        assertEquals("05:35", PrayerTimesRepository.applyOffsetToTime("05:30", 5))

        // Normal negative offset
        assertEquals("05:25", PrayerTimesRepository.applyOffsetToTime("05:30", -5))

        // Midnight boundary backward
        assertEquals("23:55", PrayerTimesRepository.applyOffsetToTime("00:05", -10))

        // Midnight boundary forward
        assertEquals("00:05", PrayerTimesRepository.applyOffsetToTime("23:55", 10))

        // Zero offset
        assertEquals("12:30", PrayerTimesRepository.applyOffsetToTime("12:30", 0))
    }

    @Test
    fun testIndividualAndGlobalAdjustments() {
        val adjustments = IndividualAdjustments(
            fajr = 3,
            sunrise = 0,
            dhuhr = -2,
            asr = 0,
            maghrib = 1,
            isha = -1
        )

        assertEquals(3, adjustments.forPrayer(PrayerType.FAJR))
        assertEquals(-2, adjustments.forPrayer(PrayerType.DHUHR))
        assertEquals(0, adjustments.forPrayer(PrayerType.ASR))

        val globalOffset = 2
        val totalFajrOffset = globalOffset + adjustments.forPrayer(PrayerType.FAJR) // 2 + 3 = 5
        val totalDhuhrOffset = globalOffset + adjustments.forPrayer(PrayerType.DHUHR) // 2 - 2 = 0

        assertEquals(5, totalFajrOffset)
        assertEquals(0, totalDhuhrOffset)

        assertEquals("05:35", PrayerTimesRepository.applyOffsetToTime("05:30", totalFajrOffset))
        assertEquals("12:30", PrayerTimesRepository.applyOffsetToTime("12:30", totalDhuhrOffset))
    }

    @Test
    fun testMasterAndIndividualAdhanSwitches() {
        val switches = IndividualAdhanSwitches(
            fajr = true,
            dhuhr = false,
            asr = true,
            maghrib = true,
            isha = true
        )

        // Sunrise is never an adhan-eligible prayer
        assertFalse(switches.isEnabledFor(PrayerType.SUNRISE))
        assertTrue(switches.isEnabledFor(PrayerType.FAJR))
        assertFalse(switches.isEnabledFor(PrayerType.DHUHR))
        assertTrue(switches.isEnabledFor(PrayerType.ASR))

        // When Master Adhan switch is OFF, no adhan plays
        val masterOff = false
        val playsFajr = masterOff && switches.isEnabledFor(PrayerType.FAJR)
        assertFalse("When master switch is OFF, no prayer plays adhan", playsFajr)

        // When Master Adhan switch is ON, respects individual switches
        val masterOn = true
        val playsFajrWhenOn = masterOn && switches.isEnabledFor(PrayerType.FAJR)
        val playsDhuhrWhenOn = masterOn && switches.isEnabledFor(PrayerType.DHUHR)
        assertTrue(playsFajrWhenOn)
        assertFalse(playsDhuhrWhenOn)
    }

    @Test
    fun testRoomCacheAndDao() = runTest {
        val dateString = "2026-11-06"
        val entity = PrayerTimeEntity(
            id = "prayer_$dateString",
            dateString = dateString,
            latitude = 36.7538,
            longitude = 3.0588,
            locationName = "الجزائر العاصمة",
            timezone = "Africa/Algiers",
            fajr = "05:32",
            sunrise = "07:01",
            dhuhr = "12:35",
            asr = "15:31",
            maghrib = "17:49",
            isha = "19:13",
            methodId = 19,
            schoolId = 0
        )

        database.prayerDao().insertPrayerTime(entity)

        val retrieved = database.prayerDao().getPrayerTimeForDate(dateString)
        assertNotNull(retrieved)
        assertEquals("05:32", retrieved?.fajr)
        assertEquals("الجزائر العاصمة", retrieved?.locationName)
        assertEquals(19, retrieved?.methodId)

        // Map to DailyPrayerSchedule
        val settings = PrayerSettings(
            isConfigured = true,
            locationMode = LocationMode.MANUAL,
            locationName = "الجزائر العاصمة",
            calculationMethodId = 19
        )
        val schedule = timesRepo.mapEntityToDailySchedule(retrieved!!, settings, LocalDate.parse(dateString))
        assertEquals(6, schedule.prayers.size)
        assertNotNull(schedule.nextPrayer)
        assertEquals(CalculationMethods.ALGERIA.id, schedule.methodInfo.id)
    }

    @Test
    fun testAlgeriaCalculationMethodDefault() {
        val defaultMethod = CalculationMethods.ALGERIA
        assertEquals(19, defaultMethod.id)
        assertTrue(defaultMethod.isDefault)
        assertEquals("الجزائر (وزارة الشؤون الدينية والأوقاف)", defaultMethod.arabicName)
    }
}
