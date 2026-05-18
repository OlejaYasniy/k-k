package com.example.indoornavigation.data.local

import androidx.room.*
import com.example.indoornavigation.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert
    suspend fun insert(item: SearchHistoryEntity)

    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT 50")
    fun getByUser(userId: Int): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearByUser(userId: Int)

    @Delete
    suspend fun delete(item: SearchHistoryEntity)
}