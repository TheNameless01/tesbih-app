package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counter_session",
    indices = [
        Index(value = ["status"]),
        Index(value = ["dhikrId"])
    ]
)
data class CounterSessionEntity(
    @PrimaryKey
    val id: String,
    val dhikrId: String,
    val currentCount: Long = 0L,
    val targetCount: Long? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val status: String = "ACTIVE" // ACTIVE, COMPLETED, PAUSED
)
