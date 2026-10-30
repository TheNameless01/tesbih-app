package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.DhikrCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM dhikr_category ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<DhikrCategoryEntity>>

    @Query("SELECT * FROM dhikr_category ORDER BY sortOrder ASC")
    suspend fun getAllCategoriesList(): List<DhikrCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<DhikrCategoryEntity>)

    @Query("SELECT COUNT(*) FROM dhikr_category")
    suspend fun getCount(): Int
}
