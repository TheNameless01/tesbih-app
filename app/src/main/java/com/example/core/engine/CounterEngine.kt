package com.example.core.engine

import androidx.room.withTransaction
import com.example.core.database.MusabbihDatabase
import com.example.core.database.entity.CounterActionEntity
import com.example.core.database.entity.CounterSessionEntity
import com.example.core.database.entity.DailySummaryEntity
import com.example.core.database.entity.DhikrEntity
import com.example.core.database.entity.SessionDailyTotalsEntity
import com.example.core.ui.ArabicUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class CounterUiState(
    val sessionId: String = "",
    val dhikr: DhikrEntity? = null,
    val count: Long = 0L,
    val target: Long? = null,
    val undoAvailable: Boolean = false,
    val targetReached: Boolean = false,
    val progress: Float = 0f,
    val isLoading: Boolean = true
)

sealed interface CounterEvent {
    data class TargetCrossed(val target: Long) : CounterEvent
    data class Error(val message: String) : CounterEvent
}

class CounterEngine(
    private val database: MusabbihDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val counterDao = database.counterDao()
    private val dhikrDao = database.dhikrDao()

    private val _uiState = MutableStateFlow(CounterUiState())
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CounterEvent>()
    val events: SharedFlow<CounterEvent> = _events.asSharedFlow()

    private val actionMutex = Mutex()

    init {
        scope.launch {
            initializeOrRestoreSession()
        }
    }

    suspend fun initializeOrRestoreSession() = actionMutex.withLock {
        _uiState.update { it.copy(isLoading = true) }

        var activeSession = counterDao.getActiveSession()
        if (activeSession == null) {
            // Find default dhikr (e.g. SubhanAllah wa bihamdihi)
            val allDhikrs = database.dhikrDao().getAllCustomDhikrs()
            val defaultDhikr = dhikrDao.getDhikrById("dhikr_tasbeeh_01")
                ?: dhikrDao.getDhikrById("dhikr_tasbeeh_03")

            val dhikrId = defaultDhikr?.id ?: "dhikr_tasbeeh_01"
            val target = defaultDhikr?.defaultTarget ?: 100L

            val newSession = CounterSessionEntity(
                id = UUID.randomUUID().toString(),
                dhikrId = dhikrId,
                currentCount = 0L,
                targetCount = target,
                startedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                status = "ACTIVE"
            )
            counterDao.insertSession(newSession)
            activeSession = newSession
        }

        val dhikr = dhikrDao.getDhikrById(activeSession.dhikrId)
        val undoable = counterDao.getLastIncrementAction(activeSession.id) != null
        val target = activeSession.targetCount
        val progress = if (target != null && target > 0) {
            (activeSession.currentCount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        } else 0f

        _uiState.value = CounterUiState(
            sessionId = activeSession.id,
            dhikr = dhikr,
            count = activeSession.currentCount,
            target = target,
            undoAvailable = undoable,
            targetReached = target != null && activeSession.currentCount >= target,
            progress = progress,
            isLoading = false
        )
    }

    suspend fun selectDhikr(dhikrId: String, customTarget: Long? = null) = actionMutex.withLock {
        val dhikr = dhikrDao.getDhikrById(dhikrId) ?: return@withLock
        val target = customTarget ?: dhikr.defaultTarget

        // Pause existing active sessions
        val currentSessionId = _uiState.value.sessionId
        val newSessionId = UUID.randomUUID().toString()

        val newSession = CounterSessionEntity(
            id = newSessionId,
            dhikrId = dhikrId,
            currentCount = 0L,
            targetCount = target,
            startedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = "ACTIVE"
        )

        database.withTransaction {
            counterDao.pauseOtherActiveSessions(newSessionId)
            counterDao.insertSession(newSession)
        }

        _uiState.value = CounterUiState(
            sessionId = newSessionId,
            dhikr = dhikr,
            count = 0L,
            target = target,
            undoAvailable = false,
            targetReached = false,
            progress = 0f,
            isLoading = false
        )
    }

    suspend fun setTarget(newTarget: Long?) = actionMutex.withLock {
        val state = _uiState.value
        if (state.sessionId.isBlank()) return@withLock

        val session = counterDao.getSessionById(state.sessionId) ?: return@withLock
        val updatedSession = session.copy(
            targetCount = newTarget,
            updatedAt = System.currentTimeMillis()
        )
        counterDao.updateSession(updatedSession)

        val progress = if (newTarget != null && newTarget > 0) {
            (state.count.toFloat() / newTarget.toFloat()).coerceIn(0f, 1f)
        } else 0f

        _uiState.update {
            it.copy(
                target = newTarget,
                targetReached = newTarget != null && it.count >= newTarget,
                progress = progress
            )
        }
    }

    /**
     * Atomic increment operation.
     * Executes in database transaction and emits milestone event if target is crossed.
     */
    suspend fun increment(): Boolean = actionMutex.withLock {
        val state = _uiState.value
        val sessionId = state.sessionId
        if (sessionId.isBlank()) return@withLock false

        val todayDate = ArabicUtils.todayLocalDateString()
        val tzId = ZoneId.systemDefault().id
        val now = System.currentTimeMillis()

        var justCrossedTarget = false
        var newCount = 0L
        var targetValue: Long? = null

        try {
            database.withTransaction {
                val session = counterDao.getSessionById(sessionId)
                    ?: throw IllegalStateException("Session not found")

                val prevCount = session.currentCount
                // Overflow protection
                if (prevCount >= Long.MAX_VALUE - 1) return@withTransaction

                newCount = prevCount + 1
                targetValue = session.targetCount

                if (targetValue != null && prevCount < targetValue!! && newCount >= targetValue!!) {
                    justCrossedTarget = true
                }

                // 1. Record counter action
                val action = CounterActionEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    actionType = "INCREMENT",
                    previousCount = prevCount,
                    newCount = newCount,
                    createdAt = now
                )
                counterDao.insertAction(action)

                // 2. Update session count
                counterDao.updateSession(
                    session.copy(
                        currentCount = newCount,
                        updatedAt = now
                    )
                )

                // 3. Update session_daily_totals for today
                val existingDailyTotal = counterDao.getSessionDailyTotal(sessionId, todayDate)
                val currentRepetitions = existingDailyTotal?.repetitions ?: 0L
                counterDao.insertOrUpdateDailyTotal(
                    SessionDailyTotalsEntity(
                        sessionId = sessionId,
                        localDate = todayDate,
                        timezoneId = tzId,
                        repetitions = currentRepetitions + 1
                    )
                )
            }

            // Update UI State atomically
            val progress = if (targetValue != null && targetValue!! > 0) {
                (newCount.toFloat() / targetValue!!.toFloat()).coerceIn(0f, 1f)
            } else 0f

            _uiState.update {
                it.copy(
                    count = newCount,
                    undoAvailable = true,
                    targetReached = targetValue != null && newCount >= targetValue!!,
                    progress = progress
                )
            }

            if (justCrossedTarget && targetValue != null) {
                _events.emit(CounterEvent.TargetCrossed(targetValue!!))
            }

            return@withLock true
        } catch (e: Exception) {
            _events.emit(CounterEvent.Error("خطأ في حفظ التسبيح"))
            return@withLock false
        }
    }

    /**
     * Atomic undo operation.
     */
    suspend fun undo(): Boolean = actionMutex.withLock {
        val state = _uiState.value
        val sessionId = state.sessionId
        if (sessionId.isBlank()) return@withLock false

        val todayDate = ArabicUtils.todayLocalDateString()
        val now = System.currentTimeMillis()

        try {
            var restoredCount = 0L
            var targetValue: Long? = null
            var remainingUndo = false

            database.withTransaction {
                val lastAction = counterDao.getLastIncrementAction(sessionId)
                    ?: return@withTransaction

                val session = counterDao.getSessionById(sessionId)
                    ?: return@withTransaction

                restoredCount = (lastAction.previousCount).coerceAtLeast(0L)
                targetValue = session.targetCount

                // Delete last action
                counterDao.deleteActionById(lastAction.id)

                // Update session
                counterDao.updateSession(
                    session.copy(
                        currentCount = restoredCount,
                        updatedAt = now
                    )
                )

                // Decrement daily total for today
                val dailyTotal = counterDao.getSessionDailyTotal(sessionId, todayDate)
                if (dailyTotal != null && dailyTotal.repetitions > 0) {
                    counterDao.insertOrUpdateDailyTotal(
                        dailyTotal.copy(repetitions = dailyTotal.repetitions - 1)
                    )
                }

                remainingUndo = counterDao.getLastIncrementAction(sessionId) != null
            }

            val progress = if (targetValue != null && targetValue!! > 0) {
                (restoredCount.toFloat() / targetValue!!.toFloat()).coerceIn(0f, 1f)
            } else 0f

            _uiState.update {
                it.copy(
                    count = restoredCount,
                    undoAvailable = remainingUndo,
                    targetReached = targetValue != null && restoredCount >= targetValue!!,
                    progress = progress
                )
            }
            return@withLock true
        } catch (e: Exception) {
            _events.emit(CounterEvent.Error("خطأ أثناء التراجع"))
            return@withLock false
        }
    }

    /**
     * Reset counter to zero after confirmation.
     */
    suspend fun reset(): Boolean = actionMutex.withLock {
        val state = _uiState.value
        val sessionId = state.sessionId
        if (sessionId.isBlank()) return@withLock false

        val now = System.currentTimeMillis()

        try {
            database.withTransaction {
                val session = counterDao.getSessionById(sessionId) ?: return@withTransaction

                // Record reset action
                val action = CounterActionEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    actionType = "RESET",
                    previousCount = session.currentCount,
                    newCount = 0L,
                    createdAt = now
                )
                counterDao.insertAction(action)

                counterDao.updateSession(
                    session.copy(
                        currentCount = 0L,
                        updatedAt = now
                    )
                )
                // Clear undoable actions for this session
                counterDao.deleteActionsForSession(sessionId)
            }

            _uiState.update {
                it.copy(
                    count = 0L,
                    undoAvailable = false,
                    targetReached = false,
                    progress = 0f
                )
            }
            return@withLock true
        } catch (e: Exception) {
            return@withLock false
        }
    }

    /**
     * Complete the session as a separate action.
     */
    suspend fun completeSession(): Boolean = actionMutex.withLock {
        val state = _uiState.value
        val sessionId = state.sessionId
        if (sessionId.isBlank()) return@withLock false

        val now = System.currentTimeMillis()

        try {
            database.withTransaction {
                val session = counterDao.getSessionById(sessionId) ?: return@withTransaction
                counterDao.updateSession(
                    session.copy(
                        status = "COMPLETED",
                        completedAt = now,
                        updatedAt = now
                    )
                )

                // Start a fresh active session for the same dhikr
                val newSession = CounterSessionEntity(
                    id = UUID.randomUUID().toString(),
                    dhikrId = session.dhikrId,
                    currentCount = 0L,
                    targetCount = session.targetCount,
                    startedAt = now,
                    updatedAt = now,
                    status = "ACTIVE"
                )
                counterDao.insertSession(newSession)

                val dhikr = dhikrDao.getDhikrById(session.dhikrId)
                _uiState.value = CounterUiState(
                    sessionId = newSession.id,
                    dhikr = dhikr,
                    count = 0L,
                    target = session.targetCount,
                    undoAvailable = false,
                    targetReached = false,
                    progress = 0f,
                    isLoading = false
                )
            }
            return@withLock true
        } catch (e: Exception) {
            return@withLock false
        }
    }
}
