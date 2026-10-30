package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey
    val localDate: String, // YYYY-MM-DD
    val totalRepetitions: Long,
    val completedSessions: Int,
    val uniqueDhikrCount: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
