package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.DhikrJourneyEntity
import com.example.core.database.entity.DhikrJourneyItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {

    @Query("SELECT * FROM dhikr_journey WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveJourney(): Flow<DhikrJourneyEntity?>

    @Query("SELECT * FROM dhikr_journey WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveJourney(): DhikrJourneyEntity?

    @Query("SELECT * FROM dhikr_journey WHERE collectionId = :collectionId AND status IN ('ACTIVE', 'PAUSED') ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getUnfinishedJourneyByCollection(collectionId: String): DhikrJourneyEntity?

    @Query("SELECT * FROM dhikr_journey WHERE collectionId = :collectionId AND status IN ('ACTIVE', 'PAUSED') ORDER BY updatedAt DESC LIMIT 1")
    fun observeUnfinishedJourneyByCollection(collectionId: String): Flow<DhikrJourneyEntity?>

    @Query("SELECT * FROM dhikr_journey WHERE id = :id LIMIT 1")
    suspend fun getJourneyById(id: String): DhikrJourneyEntity?

    @Query("SELECT * FROM dhikr_journey WHERE id = :id LIMIT 1")
    fun observeJourneyById(id: String): Flow<DhikrJourneyEntity?>

    @Query("SELECT * FROM dhikr_journey_item WHERE journeyId = :journeyId ORDER BY position ASC")
    fun observeJourneyItems(journeyId: String): Flow<List<DhikrJourneyItemEntity>>

    @Query("SELECT * FROM dhikr_journey_item WHERE journeyId = :journeyId ORDER BY position ASC")
    suspend fun getJourneyItems(journeyId: String): List<DhikrJourneyItemEntity>

    @Query("SELECT * FROM dhikr_journey_item WHERE journeyId = :journeyId AND position = :position LIMIT 1")
    suspend fun getJourneyItemAtPosition(journeyId: String, position: Int): DhikrJourneyItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: DhikrJourneyEntity)

    @Update
    suspend fun updateJourney(journey: DhikrJourneyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourneyItems(items: List<DhikrJourneyItemEntity>)

    @Update
    suspend fun updateJourneyItem(item: DhikrJourneyItemEntity)

    @Query("UPDATE dhikr_journey SET status = 'PAUSED', updatedAt = :updatedAt WHERE status = 'ACTIVE' AND id != :exceptId")
    suspend fun pauseOtherActiveJourneys(exceptId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM dhikr_journey ORDER BY startedAt DESC")
    fun getAllJourneys(): Flow<List<DhikrJourneyEntity>>

    @Query("SELECT * FROM dhikr_journey WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedJourneys(): Flow<List<DhikrJourneyEntity>>

    @Query("DELETE FROM dhikr_journey_item WHERE journeyId = :journeyId")
    suspend fun deleteItemsForJourney(journeyId: String)

    @Query("DELETE FROM dhikr_journey WHERE id = :id")
    suspend fun deleteJourneyById(id: String)
}
