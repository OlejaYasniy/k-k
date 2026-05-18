package com.example.indoornavigation.data.repository

import android.content.Context
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.NetworkMonitor
import com.example.indoornavigation.data.local.entity.*
import com.example.indoornavigation.data.model.*
import com.example.indoornavigation.data.remote.ApiService


class NavigationRepository(
    private val api: ApiService,
    private val db: AppDatabase,
    private val context: Context
) {
    private val cache get() = db.navCacheDao()
    private val net   get() = NetworkMonitor(context).isConnected

    
    val crossFloorEdges: List<CrossFloorEdge> = listOf(
        
        CrossFloorEdge(fromNodeId=12, fromFloorId=1, toNodeId=21, toFloorId=2, type="stairs", weight=50f),
        CrossFloorEdge(fromNodeId=21, fromFloorId=2, toNodeId=30, toFloorId=3, type="stairs", weight=50f),
        
        CrossFloorEdge(fromNodeId=40, fromFloorId=4, toNodeId=49, toFloorId=5, type="stairs", weight=50f),
    )

    

    suspend fun getBuildings(): List<Building> {
        if (net) {
            val fresh = api.getBuildings()
            cache.insertBuildings(fresh.map { it.toEntity() })
            return fresh
        }
        val cached = cache.getBuildings()
        if (cached.isEmpty()) throw Exception("Нет данных. Подключитесь к интернету.")
        return cached.map { it.toModel() }
    }

    

    suspend fun getFloors(buildingId: Int): List<Floor> {
        if (net) {
            val fresh = api.getFloors(buildingId)
            cache.clearFloors(buildingId)
            cache.insertFloors(fresh.map { it.toEntity() })
            return fresh
        }
        val cached = cache.getFloors(buildingId)
        if (cached.isEmpty()) throw Exception("Данные этажей не закэшированы.")
        return cached.map { it.toModel() }
    }

    

    suspend fun getRooms(floorId: Int): List<Room> {
        if (net) {
            val fresh = api.getRooms(floorId)
            cache.clearRooms(floorId)
            cache.insertRooms(fresh.map { it.toEntity() })
            return fresh
        }
        return cache.getRooms(floorId).map { it.toModel() }
    }

    

    suspend fun getNodes(floorId: Int): List<Node> {
        if (net) {
            val fresh = api.getNodes(floorId)
            cache.clearNodes(floorId)
            cache.insertNodes(fresh.map { it.toEntity() })
            return fresh
        }
        return cache.getNodes(floorId).map { it.toModel() }
    }

    

    suspend fun getEdges(floorId: Int): List<Edge> {
        if (net) {
            val fresh = api.getEdges(floorId)
            cache.clearEdges(floorId)
            cache.insertEdges(fresh.mapIndexed { i, e -> e.toEntity(floorId) })
            return fresh
        }
        return cache.getEdges(floorId).map { it.toModel() }
    }

    

    suspend fun getPois(floorId: Int): List<Poi> {
        if (net) {
            val fresh = api.getPois(floorId)
            cache.clearPois(floorId)
            cache.insertPois(fresh.map { it.toEntity() })
            return fresh
        }
        return cache.getPois(floorId).map { it.toModel() }
    }

    

    fun isOnline() = net

    suspend fun hasCachedData() = cache.buildingCount() > 0
}



private fun Building.toEntity() = BuildingEntity(id, name, address)
private fun Floor.toEntity()    = FloorEntity(id, buildingId, name, level)
private fun Room.toEntity()     = RoomCacheEntity(id, floorId, name, x, y, width, height)
private fun Node.toEntity()     = NodeEntity(id, floorId, x, y, type)
private fun Edge.toEntity(floorId: Int) = EdgeEntity(
    edgeKey    = "$floorId-$from-$to",
    floorId    = floorId,
    fromNodeId = from,
    toNodeId   = to,
    weight     = weight
)
private fun Poi.toEntity() = PoiEntity(id, floorId, x, y, name, type)



private fun BuildingEntity.toModel()   = Building(id, name, address)
private fun FloorEntity.toModel()      = Floor(id, buildingId, name, level)
private fun RoomCacheEntity.toModel()  = Room(id, floorId, name, x, y, width, height)
private fun NodeEntity.toModel()       = Node(id, floorId, x, y, type)
private fun EdgeEntity.toModel()       = Edge(fromNodeId, toNodeId, weight)
private fun PoiEntity.toModel()        = Poi(id, floorId, x, y, name, type)