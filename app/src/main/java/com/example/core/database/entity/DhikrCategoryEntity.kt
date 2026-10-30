package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dhikr_category")
data class DhikrCategoryEntity(
    @PrimaryKey
    val id: String,
    val nameArabic: String,
    val description: String? = null,
    val sortOrder: Int = 0
)
