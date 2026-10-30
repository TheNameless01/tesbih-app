package com.example

import android.app.Application
import com.example.core.backup.BackupManager
import com.example.core.database.DhikrSeedData
import com.example.core.database.MusabbihDatabase
import com.example.core.datastore.UserPreferencesRepository
import com.example.core.engine.CounterEngine
import com.example.core.engine.HapticFeedbackHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MusabbihApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { MusabbihDatabase.getInstance(this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val counterEngine by lazy { CounterEngine(database) }
    val hapticFeedbackHelper by lazy { HapticFeedbackHelper(this) }
    val backupManager by lazy { BackupManager(this, database) }

    override fun onCreate() {
        super.onCreate()

        // Seed initial verified data if needed
        applicationScope.launch {
            val categoryDao = database.categoryDao()
            if (categoryDao.getCount() == 0) {
                categoryDao.insertCategories(DhikrSeedData.categories)
            }

            val dhikrDao = database.dhikrDao()
            if (dhikrDao.getBuiltInCount() == 0) {
                dhikrDao.insertDhikrs(DhikrSeedData.getBuiltInDhikrs())
            }
        }
    }
}
