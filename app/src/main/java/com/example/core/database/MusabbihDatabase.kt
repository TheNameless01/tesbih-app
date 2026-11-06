package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.CategoryDao
import com.example.core.database.dao.CounterDao
import com.example.core.database.dao.DhikrDao
import com.example.core.database.dao.JourneyDao
import com.example.core.database.dao.PrayerDao
import com.example.core.database.entity.CounterActionEntity
import com.example.core.database.entity.CounterSessionEntity
import com.example.core.database.entity.DailySummaryEntity
import com.example.core.database.entity.DhikrCategoryEntity
import com.example.core.database.entity.DhikrEntity
import com.example.core.database.entity.DhikrJourneyEntity
import com.example.core.database.entity.DhikrJourneyItemEntity
import com.example.core.database.entity.PrayerTimeEntity
import com.example.core.database.entity.SessionDailyTotalsEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS dhikr_journey (
                id TEXT PRIMARY KEY NOT NULL,
                collectionId TEXT NOT NULL,
                title TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                completedAt INTEGER,
                currentItemIndex INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                totalRepetitions INTEGER NOT NULL DEFAULT 0,
                completedItemsCount INTEGER NOT NULL DEFAULT 0,
                totalItemsCount INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dhikr_journey_status ON dhikr_journey(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dhikr_journey_collectionId ON dhikr_journey(collectionId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS dhikr_journey_item (
                id TEXT PRIMARY KEY NOT NULL,
                journeyId TEXT NOT NULL,
                dhikrId TEXT NOT NULL,
                position INTEGER NOT NULL,
                targetCount INTEGER NOT NULL,
                currentCount INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'NOT_STARTED',
                startedAt INTEGER,
                completedAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dhikr_journey_item_journeyId_position ON dhikr_journey_item(journeyId, position)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dhikr_journey_item_dhikrId ON dhikr_journey_item(dhikrId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS prayer_time_cache (
                id TEXT PRIMARY KEY NOT NULL,
                dateString TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                locationName TEXT NOT NULL,
                timezone TEXT NOT NULL,
                fajr TEXT NOT NULL,
                sunrise TEXT NOT NULL,
                dhuhr TEXT NOT NULL,
                asr TEXT NOT NULL,
                maghrib TEXT NOT NULL,
                isha TEXT NOT NULL,
                methodId INTEGER NOT NULL DEFAULT 19,
                schoolId INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_prayer_time_cache_dateString ON prayer_time_cache(dateString)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_prayer_time_cache_dateString_methodId_schoolId ON prayer_time_cache(dateString, methodId, schoolId)")
    }
}

@Database(
    entities = [
        DhikrEntity::class,
        DhikrCategoryEntity::class,
        CounterSessionEntity::class,
        CounterActionEntity::class,
        SessionDailyTotalsEntity::class,
        DailySummaryEntity::class,
        DhikrJourneyEntity::class,
        DhikrJourneyItemEntity::class,
        PrayerTimeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MusabbihDatabase : RoomDatabase() {

    abstract fun dhikrDao(): DhikrDao
    abstract fun categoryDao(): CategoryDao
    abstract fun counterDao(): CounterDao
    abstract fun journeyDao(): JourneyDao
    abstract fun prayerDao(): PrayerDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
