package com.example.indoornavigation.ui.map

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.indoornavigation.R
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.SessionManager
import com.example.indoornavigation.data.local.SettingsManager
import com.example.indoornavigation.data.local.entity.FavoriteEntity
import com.example.indoornavigation.data.model.*
import com.example.indoornavigation.ui.common.UiState
import com.example.indoornavigation.ui.main.MainViewModel
import kotlinx.coroutines.flow.combine
import android.util.TypedValue
import kotlinx.coroutines.launch

class MapFragment : Fragment(R.layout.fragment_map) {

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var canvasView:  FloorCanvasView
    private lateinit var tvFloorName: TextView
    private lateinit var btnBuild:    View
    private lateinit var btnClear:    Button
    private lateinit var tvStatus:    TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvSteps:     RecyclerView
    private lateinit var floorTabs:   LinearLayout
    private lateinit var spinner:     Spinner
    private lateinit var btnFavorite: ImageView
    private lateinit var stepsCard:   View

    private var activePopup: PopupWindow? = null
    private var buildingsList: List<Building> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        canvasView  = view.findViewById(R.id.canvasView)
        tvFloorName = view.findViewById(R.id.tvFloorName)
        btnBuild    = view.findViewById(R.id.btnBuildRoute)
        btnClear    = view.findViewById(R.id.btnClearRoute)
        tvStatus    = view.findViewById(R.id.tvStatus)
        progressBar = view.findViewById(R.id.progressBar)
        rvSteps     = view.findViewById(R.id.rvSteps)
        floorTabs   = view.findViewById(R.id.floorTabs)
        spinner     = view.findViewById(R.id.spinnerBuilding)
        btnFavorite = view.findViewById(R.id.btnFavorite)
        stepsCard   = view.findViewById(R.id.stepsCard)

        rvSteps.layoutManager = LinearLayoutManager(requireContext())

        btnBuild.setOnClickListener { viewModel.buildRoute() }
        btnClear.setOnClickListener { viewModel.clearRoute() }

        
        setupBuildingSpinner()

        
        setupFavoriteButton()

        
        canvasView.onRoomClick = { room, screenX, screenY ->
            showRoomPopup(room, screenX, screenY)
        }

        observeAll()
    }

    override fun onResume() {
        super.onResume()
        
        val settings = SettingsManager(requireContext())
        canvasView.fontScale = settings.mapFontSize.scale
        canvasView.updateThemeColors()
    }

    

    private fun setupBuildingSpinner() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.buildings.collect { state ->
                if (state is UiState.Success) {
                    buildingsList = state.data
                    val names = state.data.map { com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(it.name, requireContext()) }
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        names
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    spinner.adapter = adapter

                    
                    val selected = viewModel.selectedBuilding.value
                    if (selected != null) {
                        val idx = state.data.indexOfFirst { it.id == selected.id }
                        if (idx >= 0) spinner.setSelection(idx, false)
                    }

                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?, v: View?, pos: Int, id: Long
                        ) {
                            val building = buildingsList[pos]
                            if (viewModel.selectedBuilding.value?.id != building.id) {
                                viewModel.selectBuilding(building)
                            }
                            updateFavoriteIcon()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            }
        }
    }

    

    private fun setupFavoriteButton() {
        btnFavorite.setOnClickListener {
            val building = viewModel.selectedBuilding.value ?: return@setOnClickListener
            val ctx = requireContext()
            val session = SessionManager(ctx)
            if (!session.isLoggedIn) {
                Toast.makeText(ctx,
                    "Войдите, чтобы добавить в избранное", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = AppDatabase.getInstance(ctx)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val existing = db.favoriteDao().find(session.userId, building.id)
                    if (existing != null) {
                        db.favoriteDao().delete(existing)
                        Toast.makeText(ctx,
                            R.string.map_removed_fav, Toast.LENGTH_SHORT).show()
                    } else {
                        db.favoriteDao().insert(
                            FavoriteEntity(
                                userId          = session.userId,
                                buildingId      = building.id,
                                buildingName    = building.name,
                                buildingAddress = building.address
                            )
                        )
                        Toast.makeText(ctx,
                            R.string.map_added_fav, Toast.LENGTH_SHORT).show()
                    }
                    updateFavoriteIcon()
                } catch (e: Exception) {
                    android.util.Log.e("MapFragment", "Ошибка избранного: ${e.message}", e)
                    Toast.makeText(ctx, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFavoriteIcon() {
        val building = viewModel.selectedBuilding.value ?: return
        val ctx = context ?: return
        val session = SessionManager(ctx)
        if (!session.isLoggedIn) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_outline)
            return
        }

        val db = AppDatabase.getInstance(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val existing = db.favoriteDao().find(session.userId, building.id)
                btnFavorite.setImageResource(
                    if (existing != null) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_outline
                )
            } catch (e: Exception) {
                android.util.Log.e("MapFragment", "Ошибка обновления иконки: ${e.message}", e)
            }
        }
    }

    

    private fun showRoomPopup(room: Room, sx: Float, sy: Float) {
        activePopup?.dismiss()

        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_room_action, null)

        val localizedName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(room.name, requireContext())
        popupView.findViewById<TextView>(R.id.tvPopupRoomName).text = localizedName

        val popup = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 24f
            isOutsideTouchable = true
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0))
        }

        popupView.findViewById<View>(R.id.btnPopupStart).setOnClickListener {
            viewModel.setStartRoom(room)
            popup.dismiss()
        }
        popupView.findViewById<View>(R.id.btnPopupEnd).setOnClickListener {
            viewModel.setEndRoom(room)
            popup.dismiss()
        }

        popup.setOnDismissListener { activePopup = null }
        activePopup = popup

        
        val location = IntArray(2)
        canvasView.getLocationOnScreen(location)
        val absX = location[0] + sx.toInt()
        val absY = location[1] + sy.toInt()

        
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val pw = popupView.measuredWidth
        val ph = popupView.measuredHeight

        popup.showAtLocation(
            canvasView,
            Gravity.NO_GRAVITY,
            absX - pw / 2,
            absY - ph - 24
        )
    }

    private fun updateStatusText() {
        val start = viewModel.startRoom.value
        val end   = viewModel.endRoom.value
        tvStatus.text = when {
            start != null && end != null -> {
                val sName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(start.name, requireContext())
                val eName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(end.name, requireContext())
                getString(R.string.map_status_from_to, sName, eName)
            }
            start != null -> {
                val sName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(start.name, requireContext())
                getString(R.string.map_status_from, sName)
            }
            end != null -> {
                val eName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(end.name, requireContext())
                getString(R.string.map_status_to, eName)
            }
            else -> getString(R.string.map_select_room)
        }
    }

    

    private fun observeAll() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.floors.collect { state ->
                if (state is UiState.Success) buildFloorTabs(state.data)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedFloor.collect { floor ->
                tvFloorName.text = floor?.let { com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(it.name, requireContext()) } ?: getString(R.string.map_floor_not_selected)
                refreshFloorTabHighlight(floor?.id)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayNodes.collect { canvasView.nodes = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayEdges.collect { canvasView.edges = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pois.collect { canvasView.pois = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rooms.collect { state ->
                progressBar.isVisible = state is UiState.Loading
                if (state is UiState.Success) canvasView.rooms = state.data
                if (state is UiState.Error)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayRoutePath.collect { canvasView.routePath = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.route.collect { state ->
                progressBar.isVisible = state is UiState.Loading
                stepsCard.isVisible = state is UiState.Success
                when (state) {
                    is UiState.Idle -> {
                        canvasView.routePath         = emptyList()
                        rvSteps.adapter              = null
                    }
                    is UiState.Loading -> {
                        tvStatus.text = getString(R.string.map_building_route_progress)
                    }
                    is UiState.Success -> {
                        val r = state.data
                        val sName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(r.fromRoom.name, requireContext())
                        val eName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(r.toRoom.name, requireContext())
                        val metersSuffix = if (requireContext().resources.configuration.locales[0].language == "en") "m" else "м"
                        tvStatus.text = "$sName → $eName (${String.format("%.1f", r.lengthMeters)} $metersSuffix)"
                        rvSteps.adapter = StepsAdapter(r.steps)
                    }
                    is UiState.Error -> {
                        tvStatus.text = state.message
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedBuilding.collect { updateFavoriteIcon() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.startRoom, viewModel.selectedFloor) { start, floor ->
                if (start?.floorId == floor?.id) start else null
            }.collect { canvasView.selectedStartRoom = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.endRoom, viewModel.selectedFloor) { end, floor ->
                if (end?.floorId == floor?.id) end else null
            }.collect { canvasView.selectedEndRoom = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.startRoom, viewModel.endRoom) { _, _ -> updateStatusText() }.collect {}
        }
    }

    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (requireContext().theme.resolveAttribute(attr, tv, true)) tv.data else fallback
    }

    private fun buildFloorTabs(floors: List<Floor>) {
        floorTabs.removeAllViews()
        val currentId = viewModel.selectedFloor.value?.id
        val activeColor = resolveThemeColor(com.google.android.material.R.attr.colorPrimary, 0xFF1565C0.toInt())
        val inactiveColor = resolveThemeColor(com.google.android.material.R.attr.colorPrimaryContainer, 0xFF90CAF9.toInt())
        val activeTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt())
        val inactiveTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF003C8F.toInt())
        floors.forEach { floor ->
            val isActive = floor.id == currentId
            val btn = Button(requireContext()).apply {
                text      = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(floor.name, requireContext())
                textSize  = 12f
                isAllCaps = false
                setPadding(32, 0, 32, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { marginEnd = 6 }
                setTextColor(if (isActive) activeTextColor else inactiveTextColor)
                setBackgroundColor(if (isActive) activeColor else inactiveColor)
                setOnClickListener { viewModel.selectFloor(floor) }
            }
            floorTabs.addView(btn)
        }
    }

    private fun refreshFloorTabHighlight(activeFloorId: Int?) {
        val floors = (viewModel.floors.value as? UiState.Success)?.data ?: return
        val activeColor = resolveThemeColor(com.google.android.material.R.attr.colorPrimary, 0xFF1565C0.toInt())
        val inactiveColor = resolveThemeColor(com.google.android.material.R.attr.colorPrimaryContainer, 0xFF90CAF9.toInt())
        val activeTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt())
        val inactiveTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF003C8F.toInt())
        for (i in 0 until floorTabs.childCount) {
            val btn   = floorTabs.getChildAt(i) as? Button ?: continue
            val floor = floors.getOrNull(i) ?: continue
            val isActive = floor.id == activeFloorId
            btn.setBackgroundColor(if (isActive) activeColor else inactiveColor)
            btn.setTextColor(if (isActive) activeTextColor else inactiveTextColor)
        }
    }

    override fun onDestroyView() {
        activePopup?.dismiss()
        super.onDestroyView()
    }

}