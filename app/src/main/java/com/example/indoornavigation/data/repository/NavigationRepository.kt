package com.example.indoornavigation.data.repository

import android.content.Context
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.NetworkMonitor
import com.example.indoornavigation.data.local.entity.*
import com.example.indoornavigation.data.model.*
import com.example.indoornavigation.data.remote.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class NavigationRepository(
    private val api: ApiService,
    private val db: AppDatabase,
    private val context: Context
) {
    private val cache get() = db.navCacheDao()
    private val net   get() = NetworkMonitor(context).isConnected

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    val crossFloorEdges: List<CrossFloorEdge> = emptyList()

    suspend fun getBuildings(): List<Building> {
        val cached = cache.getBuildings()
        if (cached.isNotEmpty()) {
            if (net) {
                repositoryScope.launch {
                    try {
                        val fresh = api.getBuildings()
                        cache.insertBuildings(fresh.map { it.toEntity() })
                    } catch (e: Exception) {
                        android.util.Log.w("NavigationRepository", "Background getBuildings error: ${e.message}")
                    }
                }
            }
            return cached.map { it.toModel() }
        }

        if (net) {
            try {
                val fresh = api.getBuildings()
                cache.insertBuildings(fresh.map { it.toEntity() })
                return fresh
            } catch (e: Exception) {
                android.util.Log.w("NavigationRepository", "getBuildings API error: ${e.message}")
            }
        }
        throw Exception("Нет данных. Подключитесь к интернету.")
    }

    suspend fun getFloors(buildingId: Int): List<Floor> {
        val cached = cache.getFloors(buildingId)
        if (cached.isNotEmpty()) {
            if (net) {
                repositoryScope.launch {
                    try {
                        val fresh = api.getFloors(buildingId)
                        cache.clearFloors(buildingId)
                        cache.insertFloors(fresh.map { it.toEntity() })
                    } catch (e: Exception) {
                        android.util.Log.w("NavigationRepository", "Background getFloors error: ${e.message}")
                    }
                }
            }
            return cached.map { it.toModel() }
        }

        if (net) {
            try {
                val fresh = api.getFloors(buildingId)
                cache.clearFloors(buildingId)
                cache.insertFloors(fresh.map { it.toEntity() })
                return fresh
            } catch (e: Exception) {
                android.util.Log.w("NavigationRepository", "getFloors API error: ${e.message}")
            }
        }
        throw Exception("Данные этажей не закэшированы.")
    }

    suspend fun getRooms(floorId: Int): List<Room> {
        val cached = cache.getRooms(floorId)
        if (cached.isNotEmpty()) {
            if (net) {
                repositoryScope.launch {
                    try {
                        val fresh = api.getRooms(floorId)
                        cache.clearRooms(floorId)
                        cache.insertRooms(fresh.map { it.toEntity() })
                    } catch (e: Exception) {
                        android.util.Log.w("NavigationRepository", "Background getRooms error: ${e.message}")
                    }
                }
            }
            return cached.map { it.toModel() }
        }

        if (net) {
            try {
                val fresh = api.getRooms(floorId)
                cache.clearRooms(floorId)
                cache.insertRooms(fresh.map { it.toEntity() })
                return fresh
            } catch (e: Exception) {
                android.util.Log.w("NavigationRepository", "getRooms API error: ${e.message}")
            }
        }
        return emptyList()
    }

    suspend fun getNodes(floorId: Int): List<Node> {
        val cached = cache.getNodes(floorId)
        if (cached.isNotEmpty()) {
            if (net) {
                repositoryScope.launch {
                    try {
                        val fresh = api.getNodes(floorId)
                        cache.clearNodes(floorId)
                        cache.insertNodes(fresh.map { it.toEntity() })
                    } catch (e: Exception) {
                        android.util.Log.w("NavigationRepository", "Background getNodes error: ${e.message}")
                    }
                }
            }
            return cached.map { it.toModel() }
        }

        if (net) {
            try {
                val fresh = api.getNodes(floorId)
                cache.clearNodes(floorId)
                cache.insertNodes(fresh.map { it.toEntity() })
                return fresh
            } catch (e: Exception) {
                android.util.Log.w("NavigationRepository", "getNodes API error: ${e.message}")
            }
        }
        return emptyList()
    }

    suspend fun getEdges(floorId: Int): List<Edge> {
        val cached = cache.getEdges(floorId)
        if (cached.isNotEmpty()) {
            if (net) {
                repositoryScope.launch {
                    try {
                        val fresh = api.getEdges(floorId)
                        cache.clearEdges(floorId)
                        cache.insertEdges(fresh.mapIndexed { i, e -> e.toEntity(floorId) })
                    } catch (e: Exception) {
                        android.util.Log.w("NavigationRepository", "Background getEdges error: ${e.message}")
                    }
                }
            }
            return cached.map { it.toModel() }
        }

        if (net) {
            try {
                val fresh = api.getEdges(floorId)
                cache.clearEdges(floorId)
                cache.insertEdges(fresh.mapIndexed { i, e -> e.toEntity(floorId) })
                return fresh
            } catch (e: Exception) {
                android.util.Log.w("NavigationRepository", "getEdges API error: ${e.message}")
            }
        }
        return emptyList()
    }

    suspend fun getPois(floorId: Int): List<Poi> {
        val cached = cache.getPois(floorId)
        if (cached.isNotEmpty()) {
            if (net) {
                repositoryScope.launch {
                    try {
                        val fresh = api.getPois(floorId)
                        cache.clearPois(floorId)
                        cache.insertPois(fresh.map { it.toEntity() })
                    } catch (e: Exception) {
                        android.util.Log.w("NavigationRepository", "Background getPois error: ${e.message}")
                    }
                }
            }
            return cached.map { it.toModel() }
        }

        if (net) {
            try {
                val fresh = api.getPois(floorId)
                cache.clearPois(floorId)
                cache.insertPois(fresh.map { it.toEntity() })
                return fresh
            } catch (e: Exception) {
                android.util.Log.w("NavigationRepository", "getPois API error: ${e.message}")
            }
        }
        return emptyList()
    }

    fun isOnline() = net

    suspend fun hasCachedData() = cache.buildingCount() > 0

    /** Wipes all nav cache — call after language change so next load fetches fresh localized data */
    suspend fun invalidateCache() {
        cache.clearAllBuildings()
        cache.clearAllFloors()
        cache.clearAllRooms()
        cache.clearAllNodes()
        cache.clearAllEdges()
        cache.clearAllPois()
    }
}



private fun Building.toEntity() = BuildingEntity(id, name, nameEn, address, addressEn)
private fun Floor.toEntity()    = FloorEntity(id, buildingId, name, nameEn, level)
private fun Room.toEntity()     = RoomCacheEntity(id, floorId, name, nameEn, x, y, width, height)
private fun Node.toEntity()     = NodeEntity(id, floorId, x, y, type)
private fun Edge.toEntity(floorId: Int) = EdgeEntity(
    edgeKey    = "$floorId-$from-$to",
    floorId    = floorId,
    fromNodeId = from,
    toNodeId   = to,
    weight     = weight
)
private fun Poi.toEntity() = PoiEntity(id, floorId, x, y, name, nameEn, type)



private fun BuildingEntity.toModel()   = Building(id, name, nameEn, address, addressEn)
private fun FloorEntity.toModel()      = Floor(id, buildingId, name, nameEn, level)
private fun RoomCacheEntity.toModel()  = Room(id, floorId, name, nameEn, x, y, width, height)
private fun NodeEntity.toModel()       = Node(id, floorId, x, y, type)
private fun EdgeEntity.toModel()       = Edge(fromNodeId, toNodeId, weight)
private fun PoiEntity.toModel()        = Poi(id, floorId, x, y, name, nameEn, type)