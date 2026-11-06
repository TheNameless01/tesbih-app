package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.PrayerTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTimes: List<PrayerTimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTime(prayerTime: PrayerTimeEntity)

    @Query("SELECT * FROM prayer_time_cache WHERE dateString = :dateString LIMIT 1")
    suspend fun getPrayerTimeForDate(dateString: String): PrayerTimeEntity?

    @Query("SELECT * FROM prayer_time_cache WHERE dateString = :dateString LIMIT 1")
    fun getPrayerTimeFlowForDate(dateString: String): Flow<PrayerTimeEntity?>

    @Query("SELECT * FROM prayer_time_cache WHERE dateString >= :startDateString ORDER BY dateString ASC LIMIT :limit")
    suspend fun getUpcomingPrayerTimes(startDateString: String, limit: Int = 14): List<PrayerTimeEntity>

    @Query("SELECT COUNT(*) FROM prayer_time_cache")
    suspend fun getCount(): Int

    @Query("DELETE FROM prayer_time_cache")
    suspend fun deleteAll()

    @Query("DELETE FROM prayer_time_cache WHERE dateString < :dateString")
    suspend fun deleteOlderThan(dateString: String)
}
