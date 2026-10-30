package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.CounterActionEntity
import com.example.core.database.entity.CounterSessionEntity
import com.example.core.database.entity.DailySummaryEntity
import com.example.core.database.entity.SessionDailyTotalsEntity
import kotlinx.coroutines.flow.Flow

data class DhikrUsageStat(
    val dhikrId: String,
    val arabicText: String,
    val title: String?,
    val totalCount: Long
)

data class DayRepetitionStat(
    val localDate: String,
    val totalRepetitions: Long
)

@Dao
interface CounterDao {

    // Active session
    @Query("SELECT * FROM counter_session WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<CounterSessionEntity?>

    @Query("SELECT * FROM counter_session WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSession(): CounterSessionEntity?

    @Query("SELECT * FROM counter_session WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): CounterSessionEntity?

    @Query("SELECT * FROM counter_session WHERE id = :id LIMIT 1")
    fun observeSessionById(id: String): Flow<CounterSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CounterSessionEntity)

    @Update
    suspend fun updateSession(session: CounterSessionEntity)

    @Query("UPDATE counter_session SET status = 'PAUSED', updatedAt = :updatedAt WHERE status = 'ACTIVE' AND id != :exceptId")
    suspend fun pauseOtherActiveSessions(exceptId: String, updatedAt: Long = System.currentTimeMillis())

    // History
    @Query("""
        SELECT * FROM counter_session 
        ORDER BY startedAt DESC
    """)
    fun getAllSessions(): Flow<List<CounterSessionEntity>>

    @Query("""
        SELECT * FROM counter_session 
        WHERE startedAt >= :sinceTimestamp 
        ORDER BY startedAt DESC
    """)
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<CounterSessionEntity>>

    // Counter actions (undo stack)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: CounterActionEntity)

    @Query("SELECT * FROM counter_action WHERE sessionId = :sessionId AND actionType = 'INCREMENT' ORDER BY createdAt DESC LIMIT 20")
    suspend fun getUndoableActions(sessionId: String): List<CounterActionEntity>

    @Query("SELECT * FROM counter_action WHERE sessionId = :sessionId AND actionType = 'INCREMENT' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastIncrementAction(sessionId: String): CounterActionEntity?

    @Query("DELETE FROM counter_action WHERE id = :id")
    suspend fun deleteActionById(id: String)

    @Query("DELETE FROM counter_action WHERE sessionId = :sessionId")
    suspend fun deleteActionsForSession(sessionId: String)

    // Per-day session totals
    @Query("SELECT * FROM session_daily_totals WHERE sessionId = :sessionId AND localDate = :localDate LIMIT 1")
    suspend fun getSessionDailyTotal(sessionId: String, localDate: String): SessionDailyTotalsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyTotal(total: SessionDailyTotalsEntity)

    @Query("SELECT SUM(repetitions) FROM session_daily_totals")
    fun observeTotalRepetitions(): Flow<Long?>

    @Query("SELECT SUM(repetitions) FROM session_daily_totals")
    suspend fun getTotalRepetitionsAllTime(): Long?

    @Query("SELECT SUM(repetitions) FROM session_daily_totals WHERE localDate = :localDate")
    fun observeTodayRepetitions(localDate: String): Flow<Long?>

    @Query("SELECT localDate, SUM(repetitions) as totalRepetitions FROM session_daily_totals WHERE localDate >= :startDate GROUP BY localDate ORDER BY localDate ASC")
    fun getDailyRepetitionsSince(startDate: String): Flow<List<DayRepetitionStat>>

    @Query("SELECT DISTINCT localDate FROM session_daily_totals WHERE repetitions > 0 ORDER BY localDate DESC")
    suspend fun getAllActiveDates(): List<String>

    @Query("""
        SELECT d.id AS dhikrId, d.arabicText AS arabicText, d.title AS title, SUM(s.currentCount) AS totalCount
        FROM counter_session s
        INNER JOIN dhikr d ON s.dhikrId = d.id
        WHERE s.currentCount > 0
        GROUP BY s.dhikrId
        ORDER BY totalCount DESC
        LIMIT 5
    """)
    fun getMostUsedDhikr(): Flow<List<DhikrUsageStat>>

    @Query("SELECT COUNT(DISTINCT dhikrId) FROM counter_session WHERE currentCount > 0")
    fun observeUniqueDhikrPracticedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM counter_session WHERE status = 'COMPLETED'")
    fun observeCompletedSessionsCount(): Flow<Int>

    // Daily summary table
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summary WHERE localDate = :localDate LIMIT 1")
    suspend fun getDailySummary(localDate: String): DailySummaryEntity?

    @Query("SELECT * FROM counter_session")
    suspend fun getAllSessionsList(): List<CounterSessionEntity>

    @Query("SELECT * FROM session_daily_totals")
    suspend fun getAllSessionDailyTotalsList(): List<SessionDailyTotalsEntity>

    @Query("DELETE FROM counter_session")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM counter_action")
    suspend fun deleteAllActions()

    @Query("DELETE FROM session_daily_totals")
    suspend fun deleteAllDailyTotals()

    @Query("DELETE FROM daily_summary")
    suspend fun deleteAllDailySummaries()
}
