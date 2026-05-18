package com.example.indoornavigation.data.local

import androidx.room.*
import com.example.indoornavigation.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavoriteEntity)

    @Delete
    suspend fun delete(fav: FavoriteEntity)

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    fun getByUser(userId: Int): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND buildingId = :buildingId LIMIT 1")
    suspend fun find(userId: Int, buildingId: Int): FavoriteEntity?
}