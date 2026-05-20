package com.example.indoornavigation.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.NetworkMonitor
import com.example.indoornavigation.data.local.SessionManager
import com.example.indoornavigation.data.local.entity.SearchHistoryEntity
import com.example.indoornavigation.data.model.*
import com.example.indoornavigation.data.repository.NavigationRepository
import com.example.indoornavigation.routing.InstructionEngine
import com.example.indoornavigation.routing.RouteEngine
import com.example.indoornavigation.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.hypot

class MainViewModel(
    app: Application,
    private val repository: NavigationRepository
) : AndroidViewModel(app) {

    private val _buildings = MutableStateFlow<UiState<List<Building>>>(UiState.Idle)
    val buildings: StateFlow<UiState<List<Building>>> = _buildings

    // Emits Unit when language changes — fragments collect this to trigger reload
    private val _languageChanged = MutableSharedFlow<Unit>(replay = 0)
    val languageChanged: SharedFlow<Unit> = _languageChanged.asSharedFlow()

    fun notifyLanguageChanged() {
        viewModelScope.launch { _languageChanged.emit(Unit) }
    }

    private val _floors = MutableStateFlow<UiState<List<Floor>>>(UiState.Idle)
    val floors: StateFlow<UiState<List<Floor>>> = _floors

    private val _selectedBuilding = MutableStateFlow<Building?>(null)
    val selectedBuilding: StateFlow<Building?> = _selectedBuilding

    private val _selectedFloor = MutableStateFlow<Floor?>(null)
    val selectedFloor: StateFlow<Floor?> = _selectedFloor

    private val _rooms = MutableStateFlow<UiState<List<Room>>>(UiState.Idle)
    val rooms: StateFlow<UiState<List<Room>>> = _rooms

    private val _displayNodes = MutableStateFlow<List<Node>>(emptyList())
    val displayNodes: StateFlow<List<Node>> = _displayNodes

    private val _displayEdges = MutableStateFlow<List<Edge>>(emptyList())
    val displayEdges: StateFlow<List<Edge>> = _displayEdges

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())
    val pois: StateFlow<List<Poi>> = _pois

    private val _startRoom = MutableStateFlow<Room?>(null)
    val startRoom: StateFlow<Room?> = _startRoom

    private val _endRoom = MutableStateFlow<Room?>(null)
    val endRoom: StateFlow<Room?> = _endRoom

    private val _route = MutableStateFlow<UiState<RouteResult>>(UiState.Idle)
    val route: StateFlow<UiState<RouteResult>> = _route

    private val _displayRoutePath = MutableStateFlow<List<Node>>(emptyList())
    val displayRoutePath: StateFlow<List<Node>> = _displayRoutePath

    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    

    fun loadBuildings() {
        viewModelScope.launch {
            _buildings.value = UiState.Loading
            _isOnline.value  = repository.isOnline()
            try {
                val list = repository.getBuildings()
                _buildings.value = UiState.Success(list)
                
                // Complete background pre-caching of ALL buildings and their floors/rooms/nodes/edges/pois
                if (repository.isOnline()) {
                    launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (b in list) {
                            try {
                                val floorsList = repository.getFloors(b.id)
                                for (floor in floorsList) {
                                    repository.getRooms(floor.id)
                                    repository.getNodes(floor.id)
                                    repository.getEdges(floor.id)
                                    repository.getPois(floor.id)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainViewModel", "Ошибка фонового кэширования здания ${b.name}: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _buildings.value = UiState.Error(e.message ?: "Ошибка загрузки зданий")
            }
        }
    }

    /** Force fresh load from server — used after language change (cache was already cleared) */
    fun reloadBuildings() {
        viewModelScope.launch {
            _buildings.value = UiState.Loading
            _isOnline.value  = repository.isOnline()
            try {
                val list = repository.getBuildings()
                _buildings.value = UiState.Success(list)
                if (repository.isOnline()) {
                    launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (b in list) {
                            try {
                                val floorsList = repository.getFloors(b.id)
                                for (floor in floorsList) {
                                    repository.getRooms(floor.id)
                                    repository.getNodes(floor.id)
                                    repository.getEdges(floor.id)
                                    repository.getPois(floor.id)
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("MainViewModel", "Lang reload cache error: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _buildings.value = UiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun refreshSelectedData() {
        viewModelScope.launch {
            _buildings.value = UiState.Loading
            _isOnline.value  = repository.isOnline()
            try {
                val list = repository.getBuildings()
                _buildings.value = UiState.Success(list)

                // Trigger background pre-caching of all buildings and their floors/rooms/nodes/edges/pois when online
                if (repository.isOnline()) {
                    launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (b in list) {
                            try {
                                val floorsList = repository.getFloors(b.id)
                                for (floor in floorsList) {
                                    repository.getRooms(floor.id)
                                    repository.getNodes(floor.id)
                                    repository.getEdges(floor.id)
                                    repository.getPois(floor.id)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainViewModel", "Ошибка фонового кэширования здания ${b.name}: ${e.message}")
                            }
                        }
                    }
                }

                val currentB = _selectedBuilding.value
                if (currentB != null) {
                    val updatedB = list.find { it.id == currentB.id }
                    if (updatedB != null) {
                        _selectedBuilding.value = updatedB
                    }
                }
            } catch (e: Exception) {
                _buildings.value = UiState.Error(e.message ?: "Ошибка загрузки зданий")
            }

            val currentB = _selectedBuilding.value
            if (currentB != null) {
                _floors.value = UiState.Loading
                try {
                    val floorsList = repository.getFloors(currentB.id)
                    _floors.value = UiState.Success(floorsList)
                    val currentF = _selectedFloor.value
                    if (currentF != null) {
                        val updatedF = floorsList.find { it.id == currentF.id }
                        if (updatedF != null) {
                            _selectedFloor.value = updatedF
                            loadFloorData(updatedF.id)
                        }
                    }
                } catch (e: Exception) {
                    _floors.value = UiState.Error(e.message ?: "Ошибка загрузки этажей")
                }
            }

            
            val activeRoute = (_route.value as? UiState.Success)?.data
            if (activeRoute != null) {
                buildRoute(saveToHistory = false)
            }
        }
    }

    fun selectBuilding(building: Building) {
        _selectedBuilding.value = building
        _selectedFloor.value    = null
        _startRoom.value        = null
        _endRoom.value          = null
        _route.value            = UiState.Idle
        _displayRoutePath.value = emptyList()
        _displayNodes.value     = emptyList()
        _displayEdges.value     = emptyList()
        loadFloors(building.id)
    }

    fun selectBuildingById(buildingId: Int) {
        viewModelScope.launch {
            try {
                val list = repository.getBuildings()
                val building = list.find { it.id == buildingId }
                if (building != null) {
                    selectBuilding(building)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rebuildRouteFromHistory(history: SearchHistoryEntity) {
        viewModelScope.launch {
            try {
                val allBuildings = repository.getBuildings()
                val building = allBuildings.find { it.name == history.buildingName } ?: return@launch
                
                _selectedBuilding.value = building
                
                val floorsList = repository.getFloors(building.id)
                _floors.value = UiState.Success(floorsList)
                
                var startRoomFound: Room? = null
                var endRoomFound: Room? = null
                
                for (floor in floorsList) {
                    val roomsList = repository.getRooms(floor.id)
                    if (startRoomFound == null) {
                        startRoomFound = roomsList.find { it.name == history.fromRoomName }
                    }
                    if (endRoomFound == null) {
                        endRoomFound = roomsList.find { it.name == history.toRoomName }
                    }
                }
                
                if (startRoomFound != null && endRoomFound != null) {
                    _startRoom.value = startRoomFound
                    _endRoom.value   = endRoomFound
                    
                    val startFloor = floorsList.find { it.id == startRoomFound.floorId }
                    if (startFloor != null) {
                        _selectedFloor.value = startFloor
                        loadFloorData(startFloor.id)
                    }
                    
                    buildRoute(saveToHistory = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadFloors(buildingId: Int) {
        viewModelScope.launch {
            _floors.value = UiState.Loading
            try {
                val list = repository.getFloors(buildingId)
                _floors.value = UiState.Success(list)
                if (list.isNotEmpty()) selectFloor(list.first())

                // Pre-cache all floors in the background for offline capability
                if (repository.isOnline()) {
                    launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (floor in list) {
                            try {
                                repository.getRooms(floor.id)
                                repository.getNodes(floor.id)
                                repository.getEdges(floor.id)
                                repository.getPois(floor.id)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _floors.value = UiState.Error(e.message ?: "Ошибка загрузки этажей")
            }
        }
    }

    

    fun selectFloor(floor: Floor) {
        _selectedFloor.value = floor
        val builtRoute = (_route.value as? UiState.Success<RouteResult>)?.data
        if (builtRoute != null) {
            _displayRoutePath.value = builtRoute.path.filter { it.floorId == floor.id }
        }
        loadFloorData(floor.id)
    }

    fun switchDisplayFloor(floorId: Int) {
        val currentRoute = (_route.value as? UiState.Success<RouteResult>)?.data ?: return
        _displayRoutePath.value = currentRoute.path.filter { it.floorId == floorId }
        viewModelScope.launch {
            _displayNodes.value = repository.getNodes(floorId)
            _displayEdges.value = repository.getEdges(floorId)
            _rooms.value        = UiState.Success(repository.getRooms(floorId))
            _pois.value         = repository.getPois(floorId)
        }
    }

    private fun loadFloorData(floorId: Int) {
        viewModelScope.launch {
            _rooms.value = UiState.Loading
            try {
                val roomsList = repository.getRooms(floorId)
                _rooms.value        = UiState.Success(roomsList)
                _displayNodes.value = repository.getNodes(floorId)
                _displayEdges.value = repository.getEdges(floorId)
                _pois.value         = repository.getPois(floorId)

                
                val start = _startRoom.value
                if (start != null && start.floorId == floorId) {
                    val updatedStart = roomsList.find { it.id == start.id }
                    if (updatedStart != null) _startRoom.value = updatedStart
                }
                val end = _endRoom.value
                if (end != null && end.floorId == floorId) {
                    val updatedEnd = roomsList.find { it.id == end.id }
                    if (updatedEnd != null) _endRoom.value = updatedEnd
                }
            } catch (e: Exception) {
                _rooms.value = UiState.Error(e.message ?: "Ошибка загрузки данных этажа")
            }
        }
    }

    

    fun setStartRoom(room: Room) { _startRoom.value = room }
    fun setEndRoom(room: Room)   { _endRoom.value   = room }

    

    @JvmOverloads
    fun buildRoute(saveToHistory: Boolean = true) {
        val start = _startRoom.value ?: run {
            _route.value = UiState.Error("Не выбрана точка отправления")
            return
        }
        val end = _endRoom.value ?: run {
            _route.value = UiState.Error("Не выбрана точка назначения")
            return
        }

        _route.value = UiState.Loading

        viewModelScope.launch {
            try {
                val allFloors = (_floors.value as? UiState.Success)?.data ?: emptyList()
                val allNodes  = mutableListOf<Node>()
                val allEdges  = mutableListOf<Edge>()

                for (floor in allFloors) {
                    allNodes += repository.getNodes(floor.id)
                    allEdges += repository.getEdges(floor.id)
                }

                val crossEdges = repository.crossFloorEdges
                val engine     = RouteEngine(allNodes, allEdges, crossEdges)

                fun nearestNode(room: Room): Node? {
                    val cx = room.x + room.width  / 2f
                    val cy = room.y + room.height / 2f
                    return allNodes
                        .filter { it.floorId == room.floorId }
                        .minByOrNull { hypot((it.x - cx).toDouble(), (it.y - cy).toDouble()) }
                }

                val startNode = nearestNode(start) ?: run {
                    _route.value = UiState.Error("Узел не найден на этаже ${start.floorId}")
                    return@launch
                }
                val endNode = nearestNode(end) ?: run {
                    _route.value = UiState.Error("Узел не найден на этаже ${end.floorId}")
                    return@launch
                }

                val path = engine.findPath(startNode.id, endNode.id)

                if (path.isEmpty()) {
                    _route.value = UiState.Error("Маршрут не найден")
                    return@launch
                }

                val startRoomNode = Node(
                    id = -100,
                    floorId = start.floorId,
                    x = start.x + start.width / 2f,
                    y = start.y + start.height / 2f,
                    type = "room"
                )
                val endRoomNode = Node(
                    id = -200,
                    floorId = end.floorId,
                    x = end.x + end.width / 2f,
                    y = end.y + end.height / 2f,
                    type = "room"
                )

                val fullPath = listOf(startRoomNode) + path + endRoomNode
                val isEn = getApplication<android.app.Application>().resources.configuration.locales[0].language == "en"
                val sLabel = if (isEn && !start.nameEn.isNullOrBlank()) start.nameEn else start.name
                val eLabel = if (isEn && !end.nameEn.isNullOrBlank()) end.nameEn else end.name
                val steps  = InstructionEngine().buildSteps(fullPath, getApplication(), sLabel, eLabel)
                val length = engine.pathLength(fullPath)

                val currentFloorId = _selectedFloor.value?.id
                _displayRoutePath.value = fullPath.filter { it.floorId == currentFloorId }

                _route.value = UiState.Success(
                    RouteResult(
                        fromRoom     = start,
                        toRoom       = end,
                        path         = fullPath,
                        steps        = steps,
                        lengthMeters = length * 0.05f
                    )
                )

                
                if (saveToHistory) {
                    try {
                        val session = SessionManager(getApplication())
                        if (session.isLoggedIn) {
                            val db = AppDatabase.getInstance(getApplication())
                            db.searchHistoryDao().insert(
                                SearchHistoryEntity(
                                    userId       = session.userId,
                                    fromRoomName = start.name,
                                    fromRoomNameEn = start.nameEn,
                                    toRoomName   = end.name,
                                    toRoomNameEn = end.nameEn,
                                    buildingName = _selectedBuilding.value?.name ?: "Здание",
                                    buildingNameEn = _selectedBuilding.value?.nameEn
                                )
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "Ошибка сохранения в историю: ${e.message}", e)
                    }
                }

            } catch (e: Exception) {
                _route.value = UiState.Error(e.message ?: "Ошибка построения маршрута")
            }
        }
    }

    

    fun clearRoute() {
        _startRoom.value        = null
        _endRoom.value          = null
        _route.value            = UiState.Idle
        _displayRoutePath.value = emptyList()
    }

    

    class Factory(
        private val app: Application,
        private val repository: NavigationRepository
    ) : ViewModelProvider.Factory {

        override fun <T : androidx.lifecycle.ViewModel> create(
            modelClass: Class<T>
        ): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}