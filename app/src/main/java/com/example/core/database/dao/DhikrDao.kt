package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.DhikrEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DhikrDao {

    @Query("SELECT * FROM dhikr ORDER BY isFavorite DESC, isBuiltIn DESC, createdAt ASC")
    fun getAllDhikr(): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr WHERE categoryId = :categoryId ORDER BY isFavorite DESC, createdAt ASC")
    fun getDhikrByCategory(categoryId: String): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDhikr(): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr WHERE id = :id LIMIT 1")
    suspend fun getDhikrById(id: String): DhikrEntity?

    @Query("SELECT * FROM dhikr WHERE id = :id LIMIT 1")
    fun observeDhikrById(id: String): Flow<DhikrEntity?>

    @Query("""
        SELECT * FROM dhikr 
        WHERE normalizedSearchText LIKE '%' || :query || '%' 
           OR arabicText LIKE '%' || :query || '%' 
           OR (title IS NOT NULL AND title LIKE '%' || :query || '%')
        ORDER BY isFavorite DESC, createdAt ASC
    """)
    fun searchDhikr(query: String): Flow<List<DhikrEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDhikrs(dhikrs: List<DhikrEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDhikr(dhikr: DhikrEntity)

    @Update
    suspend fun updateDhikr(dhikr: DhikrEntity)

    @Query("UPDATE dhikr SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM dhikr WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomDhikr(id: String): Int

    @Query("SELECT COUNT(*) FROM dhikr WHERE isBuiltIn = 1")
    suspend fun getBuiltInCount(): Int

    @Query("SELECT * FROM dhikr WHERE isBuiltIn = 0")
    suspend fun getAllCustomDhikrs(): List<DhikrEntity>
}
