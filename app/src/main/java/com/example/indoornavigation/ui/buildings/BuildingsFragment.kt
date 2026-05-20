package com.example.indoornavigation.ui.buildings

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.indoornavigation.R
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.SessionManager
import com.example.indoornavigation.data.local.entity.FavoriteEntity
import com.example.indoornavigation.data.model.Building
import com.example.indoornavigation.ui.common.UiState
import com.example.indoornavigation.ui.main.MainViewModel
import com.example.indoornavigation.MainActivity
import com.example.indoornavigation.ui.map.MapFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BuildingsFragment : Fragment(R.layout.fragment_buildings) {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvBuildings = view.findViewById<RecyclerView>(R.id.rvBuildings)
        val rvFloors    = view.findViewById<RecyclerView>(R.id.rvFloors)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        rvBuildings.layoutManager = LinearLayoutManager(requireContext())
        rvFloors.layoutManager    = LinearLayoutManager(requireContext())

        val session = SessionManager(requireContext())
        val db      = AppDatabase.getInstance(requireContext())

        // Collect buildings state and show list
        viewLifecycleOwner.lifecycleScope.launch {
            val favoritesFlow = db.favoriteDao().getByUser(session.userId)

            combine(viewModel.buildings, favoritesFlow) { state, favList ->
                Pair(state, favList.map { it.buildingId }.toSet())
            }.collect { (state, favIds) ->
                progressBar.isVisible = state is UiState.Loading
                when (state) {
                    is UiState.Success -> {
                        rvBuildings.adapter = BuildingsAdapter(
                            items        = state.data,
                            favoriteIds  = favIds,
                            isLoggedIn   = session.isLoggedIn,
                            onClick      = { building -> viewModel.selectBuilding(building) },
                            onFavoriteToggle = { building ->
                                viewLifecycleOwner.lifecycleScope.launch {
                                    toggleFavorite(db, session, building, favIds)
                                }
                            }
                        )
                    }
                    is UiState.Error -> Toast.makeText(
                        requireContext(), state.message, Toast.LENGTH_SHORT
                    ).show()
                    else -> Unit
                }
            }
        }

        // Listen for language change signal from ViewModel → reload data
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.languageChanged.collect {
                viewModel.reloadBuildings()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.floors.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        rvFloors.isVisible = true
                        rvFloors.adapter = FloorsAdapter(state.data) { floor ->
                            viewModel.selectFloor(floor)
                            (requireActivity() as? MainActivity)
                                ?.showFragment(MapFragment(), R.id.nav_map)
                        }
                    }
                    else -> rvFloors.isVisible = false
                }
            }
        }
    }

    private suspend fun toggleFavorite(
        db: AppDatabase,
        session: SessionManager,
        building: Building,
        currentFavIds: Set<Int>
    ) {
        if (currentFavIds.contains(building.id)) {
            val existing = db.favoriteDao().find(session.userId, building.id)
            if (existing != null) {
                db.favoriteDao().delete(existing)
                Toast.makeText(requireContext(), "Убрано из избранного", Toast.LENGTH_SHORT).show()
            }
        } else {
            db.favoriteDao().insert(
                FavoriteEntity(
                    userId          = session.userId,
                    buildingId      = building.id,
                    buildingName    = building.name,
                    buildingNameEn  = building.nameEn,
                    buildingAddress = building.address,
                    buildingAddressEn = building.addressEn
                )
            )
            Toast.makeText(requireContext(), "Добавлено в избранное", Toast.LENGTH_SHORT).show()
        }
    }
}