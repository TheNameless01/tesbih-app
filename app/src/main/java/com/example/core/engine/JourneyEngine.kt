package com.example.core.engine

import androidx.room.withTransaction
import com.example.core.database.MusabbihDatabase
import com.example.core.database.entity.DhikrEntity
import com.example.core.database.entity.DhikrJourneyEntity
import com.example.core.database.entity.DhikrJourneyItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class JourneyUiState(
    val journey: DhikrJourneyEntity? = null,
    val items: List<DhikrJourneyItemEntity> = emptyList(),
    val dhikrMap: Map<String, DhikrEntity> = emptyMap(),
    val currentItemIndex: Int = 0,
    val currentItem: DhikrJourneyItemEntity? = null,
    val currentDhikr: DhikrEntity? = null,
    val isJourneyActive: Boolean = false,
    val isItemCompletedPromptVisible: Boolean = false,
    val isNextPreviewVisible: Boolean = false,
    val nextDhikr: DhikrEntity? = null,
    val nextItem: DhikrJourneyItemEntity? = null,
    val isJourneyCompleted: Boolean = false,
    val isDhikrListSheetVisible: Boolean = false,
    val isIncompleteWarningVisible: Boolean = false,
    val isOverviewVisible: Boolean = false,
    val overviewCollectionId: String? = null,
    val overviewCollectionTitle: String = "",
    val overviewItems: List<Pair<DhikrEntity, Long>> = emptyList(),
    val existingUnfinishedJourney: DhikrJourneyEntity? = null
)

sealed interface JourneyEvent {
    data class DhikrCompleted(val dhikr: DhikrEntity, val target: Long) : JourneyEvent
    data class JourneyCompleted(val journey: DhikrJourneyEntity) : JourneyEvent
    data class Error(val message: String) : JourneyEvent
}

class JourneyEngine(
    private val database: MusabbihDatabase,
    private val counterEngine: CounterEngine
) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)
    private val journeyDao = database.journeyDao()
    private val dhikrDao = database.dhikrDao()

    fun cleanup() {
        job.cancel()
    }

    private val _uiState = MutableStateFlow(JourneyUiState())
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<JourneyEvent>()
    val events: SharedFlow<JourneyEvent> = _events.asSharedFlow()

    private val journeyMutex = Mutex()

    init {
        // Observe CounterEngine count changes to sync current journey item
        scope.launch {
            counterEngine.uiState.collectLatest { counterState ->
                onCounterStateChanged(counterState)
            }
        }

        // Restore active journey if one was in progress
        scope.launch {
            restoreActiveJourney()
        }
    }

    private suspend fun restoreActiveJourney() = journeyMutex.withLock {
        val activeJourney = journeyDao.getActiveJourney() ?: return@withLock
        loadJourneyInternal(activeJourney.id)
    }

    private suspend fun onCounterStateChanged(counterState: CounterUiState) = journeyMutex.withLock {
        if (!job.isActive) return@withLock
        try {
            val currentJourney = _uiState.value.journey ?: return@withLock
            if (!_uiState.value.isJourneyActive) return@withLock

            val currentItem = _uiState.value.currentItem ?: return@withLock
            if (currentItem.dhikrId != counterState.dhikr?.id) return@withLock

            val newCount = counterState.count
            if (newCount == currentItem.currentCount) return@withLock

            val target = currentItem.targetCount
            val wasCompleted = currentItem.status == "COMPLETED"
            val isNowCompleted = target > 0 && newCount >= target

            val newStatus = when {
                isNowCompleted -> "COMPLETED"
                newCount > 0 -> "IN_PROGRESS"
                currentItem.status == "SKIPPED" -> "SKIPPED"
                else -> "NOT_STARTED"
            }

            val now = System.currentTimeMillis()
            val updatedItem = currentItem.copy(
                currentCount = newCount,
                status = newStatus,
                completedAt = if (isNowCompleted && currentItem.completedAt == null) now else currentItem.completedAt
            )

            // Update database
            journeyDao.updateJourneyItem(updatedItem)

            // Recalculate journey totals
            val allItems = journeyDao.getJourneyItems(currentJourney.id)
            val completedCount = allItems.count { it.status == "COMPLETED" }
            val totalReps = allItems.sumOf { it.currentCount }

            val updatedJourney = currentJourney.copy(
                completedItemsCount = completedCount,
                totalRepetitions = totalReps,
                updatedAt = now
            )
            journeyDao.updateJourney(updatedJourney)

            val justCrossedFirstTime = !wasCompleted && isNowCompleted

            _uiState.update { state ->
                state.copy(
                    journey = updatedJourney,
                    items = allItems,
                    currentItem = updatedItem,
                    isItemCompletedPromptVisible = if (justCrossedFirstTime) true else state.isItemCompletedPromptVisible
                )
            }

            if (justCrossedFirstTime && _uiState.value.currentDhikr != null) {
                _events.emit(JourneyEvent.DhikrCompleted(_uiState.value.currentDhikr!!, target))
            }
        } catch (e: Exception) {
            if (!job.isActive) return@withLock
            throw e
        }
    }

    /**
     * Show overview preview before starting/resuming a journey
     */
    suspend fun openJourneyOverview(collectionId: String, collectionTitle: String) = journeyMutex.withLock {
        val dhikrs = dhikrDao.getAllCustomDhikrs() // or by category
        val categoryDhikrs = if (collectionId == "cat_general") {
            dhikrs
        } else {
            // Load by category
            dhikrDao.getFavoriteDhikr() // fallback check
            val allList = mutableListOf<DhikrEntity>()
            // Query category items from database
            dhikrs.filter { it.categoryId == collectionId }
        }

        // Check if unfinished journey already exists for this collection
        val existingJourney = journeyDao.getUnfinishedJourneyByCollection(collectionId)

        // Fetch category adhkar from database
        val itemsList = if (categoryDhikrs.isNotEmpty()) {
            categoryDhikrs.map { it to (it.defaultTarget ?: 33L) }
        } else {
            // Check seed list or built-ins for this category
            val builtIns = com.example.core.database.DhikrSeedData.getBuiltInDhikrs()
                .filter { it.categoryId == collectionId }
            builtIns.map { it to (it.defaultTarget ?: 33L) }
        }

        _uiState.update {
            it.copy(
                isOverviewVisible = true,
                overviewCollectionId = collectionId,
                overviewCollectionTitle = collectionTitle,
                overviewItems = itemsList,
                existingUnfinishedJourney = existingJourney
            )
        }
    }

    fun dismissOverview() {
        _uiState.update { it.copy(isOverviewVisible = false) }
    }

    /**
     * Start a fresh journey for a collection
     */
    suspend fun startJourney(collectionId: String, collectionTitle: String, forceRestart: Boolean = false) = journeyMutex.withLock {
        _uiState.update { it.copy(isOverviewVisible = false) }

        // If resuming existing unfinished journey unless forceRestart is requested
        val existing = journeyDao.getUnfinishedJourneyByCollection(collectionId)
        if (existing != null && !forceRestart) {
            loadJourneyInternal(existing.id)
            return@withLock
        }

        // Pause other active journeys
        val newJourneyId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Get Dhikrs for this collection
        val builtIns = com.example.core.database.DhikrSeedData.getBuiltInDhikrs()
            .filter { it.categoryId == collectionId }
        val categoryDhikrs = builtIns.ifEmpty {
            // Fallback to general or tasbeeh
            com.example.core.database.DhikrSeedData.getBuiltInDhikrs().take(5)
        }

        val totalItems = categoryDhikrs.size
        val journeyItems = categoryDhikrs.mapIndexed { index, dhikr ->
            DhikrJourneyItemEntity(
                id = UUID.randomUUID().toString(),
                journeyId = newJourneyId,
                dhikrId = dhikr.id,
                position = index,
                targetCount = dhikr.defaultTarget ?: 33L,
                currentCount = 0L,
                status = "NOT_STARTED",
                startedAt = null,
                completedAt = null
            )
        }

        val journeyEntity = DhikrJourneyEntity(
            id = newJourneyId,
            collectionId = collectionId,
            title = collectionTitle,
            startedAt = now,
            updatedAt = now,
            completedAt = null,
            currentItemIndex = 0,
            status = "ACTIVE",
            totalRepetitions = 0L,
            completedItemsCount = 0,
            totalItemsCount = totalItems
        )

        database.withTransaction {
            journeyDao.pauseOtherActiveJourneys(newJourneyId, now)
            journeyDao.insertJourney(journeyEntity)
            journeyDao.insertJourneyItems(journeyItems)
        }

        loadJourneyInternal(newJourneyId)
    }

    /**
     * Resume an existing journey
     */
    suspend fun resumeJourney(journeyId: String) = journeyMutex.withLock {
        loadJourneyInternal(journeyId)
    }

    private suspend fun loadJourneyInternal(journeyId: String) {
        val journey = journeyDao.getJourneyById(journeyId) ?: return
        val items = journeyDao.getJourneyItems(journeyId)
        if (items.isEmpty()) return

        // Fetch Dhikr details
        val dhikrMap = mutableMapOf<String, DhikrEntity>()
        for (item in items) {
            dhikrDao.getDhikrById(item.dhikrId)?.let { d ->
                dhikrMap[d.id] = d
            }
        }

        val currentIndex = journey.currentItemIndex.coerceIn(0, items.size - 1)
        val currentItem = items[currentIndex]
        val currentDhikr = dhikrMap[currentItem.dhikrId]

        // Mark item as started if not started
        val now = System.currentTimeMillis()
        if (currentItem.startedAt == null) {
            val updatedItem = currentItem.copy(startedAt = now, status = if (currentItem.currentCount > 0) currentItem.status else "IN_PROGRESS")
            journeyDao.updateJourneyItem(updatedItem)
        }

        // Update journey status to ACTIVE
        val activeJourney = journey.copy(status = "ACTIVE", updatedAt = now)
        journeyDao.updateJourney(activeJourney)

        _uiState.value = JourneyUiState(
            journey = activeJourney,
            items = items,
            dhikrMap = dhikrMap,
            currentItemIndex = currentIndex,
            currentItem = currentItem,
            currentDhikr = currentDhikr,
            isJourneyActive = true,
            isItemCompletedPromptVisible = false,
            isNextPreviewVisible = false,
            isJourneyCompleted = activeJourney.status == "COMPLETED",
            isDhikrListSheetVisible = false,
            isIncompleteWarningVisible = false
        )

        // Point CounterEngine to this Dhikr & restore its count!
        counterEngine.selectDhikr(currentItem.dhikrId, currentItem.targetCount)
    }

    /**
     * Request to advance to next Dhikr.
     * If incomplete, prompts warning. If complete, shows next preview or finishes journey.
     */
    suspend fun requestNextDhikr() = journeyMutex.withLock {
        val state = _uiState.value
        val items = state.items
        val currentIndex = state.currentItemIndex
        val currentItem = state.currentItem ?: return@withLock

        // If already at the last item
        if (currentIndex >= items.size - 1) {
            // Check if all items completed or offer journey completion
            if (items.all { it.status == "COMPLETED" }) {
                finishJourneyInternal()
            } else {
                // If current item is completed or user confirms, finish or stay
                finishJourneyInternal()
            }
            return@withLock
        }

        val nextIndex = currentIndex + 1
        val nextItem = items[nextIndex]
        val nextDhikr = state.dhikrMap[nextItem.dhikrId]

        // If current item is not completed and user hasn't confirmed
        if (currentItem.status != "COMPLETED") {
            _uiState.update { it.copy(isIncompleteWarningVisible = true) }
            return@withLock
        }

        // Show Next Dhikr Preview
        _uiState.update {
            it.copy(
                isItemCompletedPromptVisible = false,
                isNextPreviewVisible = true,
                nextItem = nextItem,
                nextDhikr = nextDhikr
            )
        }
    }

    /**
     * Confirm proceeding past incomplete Dhikr
     */
    suspend fun confirmProceedIncomplete() = journeyMutex.withLock {
        _uiState.update { it.copy(isIncompleteWarningVisible = false) }
        val state = _uiState.value
        val currentIndex = state.currentItemIndex
        val items = state.items

        if (currentIndex >= items.size - 1) {
            finishJourneyInternal()
            return@withLock
        }

        val nextIndex = currentIndex + 1
        val nextItem = items[nextIndex]
        val nextDhikr = state.dhikrMap[nextItem.dhikrId]

        _uiState.update {
            it.copy(
                isNextPreviewVisible = true,
                nextItem = nextItem,
                nextDhikr = nextDhikr
            )
        }
    }

    fun dismissIncompleteWarning() {
        _uiState.update { it.copy(isIncompleteWarningVisible = false) }
    }

    /**
     * Start the next Dhikr after preview
     */
    suspend fun startNextDhikrFromPreview() = journeyMutex.withLock {
        val state = _uiState.value
        val nextIndex = state.currentItemIndex + 1
        val items = state.items
        if (nextIndex >= items.size) {
            finishJourneyInternal()
            return@withLock
        }

        navigateToItemIndex(nextIndex)
    }

    fun dismissNextPreview() {
        _uiState.update { it.copy(isNextPreviewVisible = false) }
    }

    /**
     * Move to previous Dhikr in journey
     */
    suspend fun previousDhikr() = journeyMutex.withLock {
        val state = _uiState.value
        val prevIndex = state.currentItemIndex - 1
        if (prevIndex < 0) return@withLock

        navigateToItemIndex(prevIndex)
    }

    /**
     * Skip current Dhikr
     */
    suspend fun skipCurrentDhikr() = journeyMutex.withLock {
        val state = _uiState.value
        val currentItem = state.currentItem ?: return@withLock

        val updatedItem = currentItem.copy(status = "SKIPPED")
        journeyDao.updateJourneyItem(updatedItem)

        val updatedItems = state.items.map {
            if (it.id == updatedItem.id) updatedItem else it
        }

        _uiState.update {
            it.copy(
                currentItem = updatedItem,
                items = updatedItems,
                isItemCompletedPromptVisible = false
            )
        }

        // Navigate to next
        val nextIndex = state.currentItemIndex + 1
        if (nextIndex < updatedItems.size) {
            navigateToItemIndex(nextIndex)
        } else {
            finishJourneyInternal()
        }
    }

    /**
     * Repeat current Dhikr (resets item count to 0 but preserves completed milestone in journey)
     */
    suspend fun repeatCurrentDhikr() = journeyMutex.withLock {
        _uiState.update { it.copy(isItemCompletedPromptVisible = false) }
        counterEngine.reset()
    }

    /**
     * Jump to a specific item from journey list sheet
     */
    suspend fun selectItemFromList(position: Int) = journeyMutex.withLock {
        _uiState.update { it.copy(isDhikrListSheetVisible = false) }
        navigateToItemIndex(position)
    }

    fun setDhikrListSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isDhikrListSheetVisible = visible) }
    }

    fun dismissCompletionPrompt() {
        _uiState.update { it.copy(isItemCompletedPromptVisible = false) }
    }

    /**
     * Navigate to specific item index within journey
     */
    private suspend fun navigateToItemIndex(index: Int) {
        val currentJourney = _uiState.value.journey ?: return
        val items = journeyDao.getJourneyItems(currentJourney.id)
        if (index !in items.indices) return

        val targetItem = items[index]
        val targetDhikr = _uiState.value.dhikrMap[targetItem.dhikrId]

        val now = System.currentTimeMillis()
        val updatedItem = if (targetItem.startedAt == null) {
            targetItem.copy(startedAt = now, status = if (targetItem.status == "NOT_STARTED") "IN_PROGRESS" else targetItem.status)
        } else targetItem

        if (updatedItem != targetItem) {
            journeyDao.updateJourneyItem(updatedItem)
        }

        val updatedJourney = currentJourney.copy(currentItemIndex = index, updatedAt = now)
        journeyDao.updateJourney(updatedJourney)

        val freshItems = journeyDao.getJourneyItems(currentJourney.id)

        _uiState.update {
            it.copy(
                journey = updatedJourney,
                items = freshItems,
                currentItemIndex = index,
                currentItem = updatedItem,
                currentDhikr = targetDhikr,
                isItemCompletedPromptVisible = false,
                isNextPreviewVisible = false
            )
        }

        // Set counter engine to this dhikr
        counterEngine.selectDhikr(updatedItem.dhikrId, updatedItem.targetCount)
    }

    /**
     * Complete the entire journey
     */
    private suspend fun finishJourneyInternal() {
        val journey = _uiState.value.journey ?: return
        val now = System.currentTimeMillis()

        val allItems = journeyDao.getJourneyItems(journey.id)
        val completedCount = allItems.count { it.status == "COMPLETED" }
        val totalReps = allItems.sumOf { it.currentCount }

        val finishedJourney = journey.copy(
            status = "COMPLETED",
            completedAt = now,
            updatedAt = now,
            completedItemsCount = completedCount,
            totalRepetitions = totalReps
        )
        journeyDao.updateJourney(finishedJourney)

        _uiState.update {
            it.copy(
                journey = finishedJourney,
                isJourneyCompleted = true,
                isItemCompletedPromptVisible = false,
                isNextPreviewVisible = false
            )
        }

        _events.emit(JourneyEvent.JourneyCompleted(finishedJourney))
    }

    /**
     * Exit journey and return to normal mode
     */
    suspend fun exitJourney() = journeyMutex.withLock {
        val journey = _uiState.value.journey
        if (journey != null && journey.status == "ACTIVE") {
            val paused = journey.copy(status = "PAUSED", updatedAt = System.currentTimeMillis())
            journeyDao.updateJourney(paused)
        }

        _uiState.value = JourneyUiState(
            isJourneyActive = false,
            journey = null,
            isJourneyCompleted = false
        )
    }

    /**
     * Restart current journey as a new one
     */
    suspend fun restartJourney() = journeyMutex.withLock {
        val currentJourney = _uiState.value.journey ?: return@withLock
        val collectionId = currentJourney.collectionId
        val title = currentJourney.title

        startJourney(collectionId, title, forceRestart = true)
    }
}
