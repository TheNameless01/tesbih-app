package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dhikr_journey",
    indices = [
        Index(value = ["status"]),
        Index(value = ["collectionId"])
    ]
)
data class DhikrJourneyEntity(
    @PrimaryKey
    val id: String,
    val collectionId: String,
    val title: String,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val currentItemIndex: Int = 0,
    val status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED
    val totalRepetitions: Long = 0L,
    val completedItemsCount: Int = 0,
    val totalItemsCount: Int = 0
)
