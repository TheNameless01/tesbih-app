package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.CategoryDao
import com.example.core.database.dao.CounterDao
import com.example.core.database.dao.DhikrDao
import com.example.core.database.entity.CounterActionEntity
import com.example.core.database.entity.CounterSessionEntity
import com.example.core.database.entity.DailySummaryEntity
import com.example.core.database.entity.DhikrCategoryEntity
import com.example.core.database.entity.DhikrEntity
import com.example.core.database.entity.SessionDailyTotalsEntity

@Database(
    entities = [
        DhikrEntity::class,
        DhikrCategoryEntity::class,
        CounterSessionEntity::class,
        CounterActionEntity::class,
        SessionDailyTotalsEntity::class,
        DailySummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusabbihDatabase : RoomDatabase() {

    abstract fun dhikrDao(): DhikrDao
    abstract fun categoryDao(): CategoryDao
    abstract fun counterDao(): CounterDao

    companion object {
        @Volatile
        private var INSTANCE: MusabbihDatabase? = null

        fun getInstance(context: Context): MusabbihDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusabbihDatabase::class.java,
                    "musabbih_database.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
