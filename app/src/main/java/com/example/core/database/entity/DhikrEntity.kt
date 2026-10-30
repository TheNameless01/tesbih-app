package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dhikr",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isBuiltIn"])
    ]
)
data class DhikrEntity(
    @PrimaryKey
    val id: String,
    val arabicText: String,
    val normalizedSearchText: String,
    val title: String? = null,
    val categoryId: String,
    val sourceType: String, // QURAN, HADITH, ATHAR, GENERAL, CUSTOM
    val sourceReference: String,
    val verificationStatus: String, // REVIEWED, UNREVIEWED, NEEDS_CORRECTION
    val defaultTarget: Long? = null,
    val isBuiltIn: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val contentRevision: Int = 1
)
