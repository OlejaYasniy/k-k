package com.example.indoornavigation.data.local

import androidx.room.*
import com.example.indoornavigation.data.local.entity.*

@Dao
interface NavCacheDao {

    
    @Query("SELECT * FROM buildings_cache ORDER BY id")
    suspend fun getBuildings(): List<BuildingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildings(list: List<BuildingEntity>)

    @Query("DELETE FROM buildings_cache")
    suspend fun clearBuildings()

    
    @Query("SELECT * FROM floors_cache WHERE buildingId = :buildingId ORDER BY level")
    suspend fun getFloors(buildingId: Int): List<FloorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloors(list: List<FloorEntity>)

    @Query("DELETE FROM floors_cache WHERE buildingId = :buildingId")
    suspend fun clearFloors(buildingId: Int)

    
    @Query("SELECT * FROM rooms_cache WHERE floorId = :floorId")
    suspend fun getRooms(floorId: Int): List<RoomCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(list: List<RoomCacheEntity>)

    @Query("DELETE FROM rooms_cache WHERE floorId = :floorId")
    suspend fun clearRooms(floorId: Int)

    
    @Query("SELECT * FROM nodes_cache WHERE floorId = :floorId")
    suspend fun getNodes(floorId: Int): List<NodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(list: List<NodeEntity>)

    @Query("DELETE FROM nodes_cache WHERE floorId = :floorId")
    suspend fun clearNodes(floorId: Int)

    
    @Query("SELECT * FROM edges_cache WHERE floorId = :floorId")
    suspend fun getEdges(floorId: Int): List<EdgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(list: List<EdgeEntity>)

    @Query("DELETE FROM edges_cache WHERE floorId = :floorId")
    suspend fun clearEdges(floorId: Int)

    
    @Query("SELECT * FROM pois_cache WHERE floorId = :floorId")
    suspend fun getPois(floorId: Int): List<PoiEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPois(list: List<PoiEntity>)

    @Query("DELETE FROM pois_cache WHERE floorId = :floorId")
    suspend fun clearPois(floorId: Int)

    
    @Query("SELECT COUNT(*) FROM buildings_cache")
    suspend fun buildingCount(): Int

    @Query("DELETE FROM buildings_cache")
    suspend fun clearAllBuildings()

    @Query("DELETE FROM floors_cache")
    suspend fun clearAllFloors()

    @Query("DELETE FROM rooms_cache")
    suspend fun clearAllRooms()

    @Query("DELETE FROM nodes_cache")
    suspend fun clearAllNodes()

    @Query("DELETE FROM edges_cache")
    suspend fun clearAllEdges()

    @Query("DELETE FROM pois_cache")
    suspend fun clearAllPois()
}
