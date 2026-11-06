package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dhikr_journey_item",
    indices = [
        Index(value = ["journeyId", "position"]),
        Index(value = ["dhikrId"])
    ]
)
data class DhikrJourneyItemEntity(
    @PrimaryKey
    val id: String,
    val journeyId: String,
    val dhikrId: String,
    val position: Int,
    val targetCount: Long,
    val currentCount: Long = 0L,
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED, SKIPPED
    val startedAt: Long? = null,
    val completedAt: Long? = null
)
