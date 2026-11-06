package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_time_cache",
    indices = [
        Index(value = ["dateString"]),
        Index(value = ["dateString", "methodId", "schoolId"])
    ]
)
data class PrayerTimeEntity(
    @PrimaryKey
    val id: String, // e.g. "prayer_2026-11-06"
    val dateString: String, // "YYYY-MM-DD"
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val timezone: String,
    val fajr: String, // "HH:mm"
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val methodId: Int = 19,
    val schoolId: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
