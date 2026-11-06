package com.example

import android.app.Application
import com.example.core.backup.BackupManager
import com.example.core.database.DhikrSeedData
import com.example.core.database.MusabbihDatabase
import com.example.core.datastore.UserPreferencesRepository
import com.example.core.engine.CounterEngine
import com.example.core.engine.HapticFeedbackHelper
import com.example.feature.prayer.data.location.PrayerLocationHelper
import com.example.feature.prayer.data.repository.PrayerPreferencesRepository
import com.example.feature.prayer.data.repository.PrayerTimesRepository
import com.example.feature.prayer.scheduling.PrayerAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MusabbihApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { MusabbihDatabase.getInstance(this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val counterEngine by lazy { CounterEngine(database) }
    val journeyEngine by lazy { com.example.core.engine.JourneyEngine(database, counterEngine) }
    val hapticFeedbackHelper by lazy { HapticFeedbackHelper(this) }
    val backupManager by lazy { BackupManager(this, database) }

    // Prayer Times & Adhan singletons
    val prayerPreferencesRepository by lazy { PrayerPreferencesRepository(this) }
    val prayerTimesRepository by lazy { PrayerTimesRepository(database.prayerDao(), prayerPreferencesRepository) }
    val prayerLocationHelper by lazy { PrayerLocationHelper(this) }

    override fun onCreate() {
        super.onCreate()

        // Seed initial verified data if needed
        applicationScope.launch {
            val categoryDao = database.categoryDao()
            if (categoryDao.getCount() == 0) {
                categoryDao.insertCategories(DhikrSeedData.categories)
            }

            val dhikrDao = database.dhikrDao()
            if (dhikrDao.getBuiltInCount() < DhikrSeedData.getBuiltInDhikrs().size) {
                dhikrDao.insertDhikrs(DhikrSeedData.getBuiltInDhikrs())
            }

            // Reconcile prayer alarms if configured
            PrayerAlarmScheduler.reconcileAlarms(this@MusabbihApplication)
        }
    }
}
