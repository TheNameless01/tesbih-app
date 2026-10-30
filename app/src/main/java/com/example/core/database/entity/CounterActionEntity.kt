package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counter_action",
    indices = [
        Index(value = ["sessionId", "createdAt"])
    ]
)
data class CounterActionEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val actionType: String, // INCREMENT, UNDO, RESET
    val previousCount: Long,
    val newCount: Long,
    val createdAt: Long = System.currentTimeMillis()
)
