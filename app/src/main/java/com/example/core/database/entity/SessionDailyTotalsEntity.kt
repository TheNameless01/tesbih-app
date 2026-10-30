package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "session_daily_totals",
    primaryKeys = ["sessionId", "localDate"],
    indices = [
        Index(value = ["localDate"])
    ]
)
data class SessionDailyTotalsEntity(
    val sessionId: String,
    val localDate: String, // YYYY-MM-DD
    val timezoneId: String,
    val repetitions: Long
)
