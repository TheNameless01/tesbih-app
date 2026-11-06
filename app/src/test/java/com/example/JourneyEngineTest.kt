package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.DhikrSeedData
import com.example.core.database.MusabbihDatabase
import com.example.core.engine.CounterEngine
import com.example.core.engine.JourneyEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JourneyEngineTest {

    private lateinit var database: MusabbihDatabase
    private lateinit var counterEngine: CounterEngine
    private lateinit var journeyEngine: JourneyEngine
    private lateinit var context: Context

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MusabbihDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Seed initial categories & dhikrs
        val categories = DhikrSeedData.categories
        val dhikrs = DhikrSeedData.getBuiltInDhikrs()
        database.categoryDao().insertCategories(categories)
        database.dhikrDao().insertDhikrs(dhikrs)

        counterEngine = CounterEngine(database)
        counterEngine.initializeOrRestoreSession()

        journeyEngine = JourneyEngine(database, counterEngine)
    }

    @After
    fun tearDown() {
        if (::journeyEngine.isInitialized) {
            journeyEngine.cleanup()
        }
        if (::counterEngine.isInitialized) {
            counterEngine.cleanup()
        }
        if (::database.isInitialized) {
            database.close()
        }
    }

    private suspend fun waitUntil(timeoutMs: Long = 1000L, condition: () -> Boolean): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            while (!condition()) {
                delay(20)
            }
            true
        } ?: false
    }

    @Test
    fun testStartMorningJourney() = runTest {
        journeyEngine.startJourney(
            collectionId = "cat_morning",
            collectionTitle = "أذكار الصباح",
            forceRestart = true
        )

        val state = journeyEngine.uiState.value
        assertTrue("Journey should be active", state.isJourneyActive)
        assertEquals("cat_morning", state.journey?.collectionId)
        assertEquals("أذكار الصباح", state.journey?.title)
        assertTrue("Total items should be > 0", state.items.isNotEmpty())
        assertEquals(0, state.currentItemIndex)
        assertNotNull(state.currentItem)
        assertEquals("NOT_STARTED", state.currentItem?.status)
    }

    @Test
    fun testCountingUpdatesJourneyItemAndShowsCompletionPrompt() = runTest {
        journeyEngine.startJourney(
            collectionId = "cat_morning",
            collectionTitle = "أذكار الصباح",
            forceRestart = true
        )

        val currentItem = journeyEngine.uiState.value.currentItem
        assertNotNull(currentItem)
        val target = currentItem!!.targetCount

        // Tap up to target
        for (i in 1..target) {
            counterEngine.increment()
        }

        // Wait for asynchronous flow propagation from CounterEngine to JourneyEngine
        val updated = waitUntil {
            journeyEngine.uiState.value.currentItem?.currentCount == target
        }
        assertTrue("Count should propagate to journey item", updated)

        val stateAfterTarget = journeyEngine.uiState.value
        assertEquals(target, stateAfterTarget.currentItem?.currentCount)
        assertTrue(
            "Prompt should be visible when target is reached",
            stateAfterTarget.isItemCompletedPromptVisible
        )
    }

    @Test
    fun testSkipCurrentDhikr() = runTest {
        journeyEngine.startJourney(
            collectionId = "cat_morning",
            collectionTitle = "أذكار الصباح",
            forceRestart = true
        )

        val initialIndex = journeyEngine.uiState.value.currentItemIndex
        val initialItem = journeyEngine.uiState.value.currentItem

        journeyEngine.skipCurrentDhikr()

        val stateAfterSkip = journeyEngine.uiState.value
        assertEquals(initialIndex + 1, stateAfterSkip.currentItemIndex)

        val skippedItem = stateAfterSkip.items.find { it.id == initialItem?.id }
        assertNotNull(skippedItem)
        assertEquals("SKIPPED", skippedItem?.status)
    }

    @Test
    fun testRepeatCurrentDhikr() = runTest {
        journeyEngine.startJourney(
            collectionId = "cat_morning",
            collectionTitle = "أذكار الصباح",
            forceRestart = true
        )

        counterEngine.increment()
        counterEngine.increment()
        waitUntil { counterEngine.uiState.value.count >= 2L }

        journeyEngine.repeatCurrentDhikr()

        waitUntil { counterEngine.uiState.value.count == 0L }
        assertEquals(0L, counterEngine.uiState.value.count)
        assertEquals(0L, journeyEngine.uiState.value.currentItem?.currentCount)
        assertFalse(journeyEngine.uiState.value.isItemCompletedPromptVisible)
    }

    @Test
    fun testPreviousDhikrNavigation() = runTest {
        journeyEngine.startJourney(
            collectionId = "cat_morning",
            collectionTitle = "أذكار الصباح",
            forceRestart = true
        )

        // Advance
        journeyEngine.skipCurrentDhikr()
        assertEquals(1, journeyEngine.uiState.value.currentItemIndex)

        // Go back
        journeyEngine.previousDhikr()
        assertEquals(0, journeyEngine.uiState.value.currentItemIndex)
    }

    @Test
    fun testResumeJourneyPreservesState() = runTest {
        journeyEngine.startJourney(
            collectionId = "cat_morning",
            collectionTitle = "أذكار الصباح",
            forceRestart = true
        )

        counterEngine.increment()
        counterEngine.increment()

        // Read active journey from DB
        val activeJourney = database.journeyDao().getActiveJourney()
        assertNotNull(activeJourney)
        assertEquals("cat_morning", activeJourney?.collectionId)

        // Resume journey in engine
        journeyEngine.resumeJourney(activeJourney!!.id)
        val resumedState = journeyEngine.uiState.value
        assertTrue(resumedState.isJourneyActive)
        assertEquals(0, resumedState.currentItemIndex)
    }
}
