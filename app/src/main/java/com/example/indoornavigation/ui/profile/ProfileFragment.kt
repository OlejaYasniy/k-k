package com.example.indoornavigation.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.indoornavigation.MainActivity
import com.example.indoornavigation.R
import com.example.indoornavigation.data.local.SessionManager
import com.example.indoornavigation.data.local.entity.FavoriteEntity
import com.example.indoornavigation.data.local.entity.SearchHistoryEntity
import com.example.indoornavigation.ui.auth.LoginFragment
import com.example.indoornavigation.ui.auth.RegisterFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(requireActivity().application)
    }

    private val mainViewModel: com.example.indoornavigation.ui.main.MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvUsername      = view.findViewById<TextView>(R.id.tvUsername)
        val tvEmail         = view.findViewById<TextView>(R.id.tvEmail)
        val guestBanner     = view.findViewById<LinearLayout>(R.id.guestBanner)
        val loggedInContent = view.findViewById<LinearLayout>(R.id.loggedInContent)
        val btnGoLogin      = view.findViewById<MaterialButton>(R.id.btnGoLogin)
        val btnGoRegister   = view.findViewById<MaterialButton>(R.id.btnGoRegister)

        val session = SessionManager(requireContext())

        if (!session.isLoggedIn) {
            
            tvUsername.text = getString(R.string.profile_guest)
            tvEmail.text   = getString(R.string.profile_sign_in_prompt)
            guestBanner.isVisible     = true
            loggedInContent.isVisible = false

            btnGoLogin.setOnClickListener {
                (requireActivity() as? MainActivity)
                    ?.showFragment(LoginFragment(), R.id.nav_profile)
            }
            btnGoRegister.setOnClickListener {
                (requireActivity() as? MainActivity)
                    ?.showFragment(RegisterFragment(), R.id.nav_profile)
            }
            return
        }

        
        guestBanner.isVisible     = false
        loggedInContent.isVisible = true

        tvUsername.text = session.username
        tvEmail.text   = session.email

        val tabLayout  = view.findViewById<TabLayout>(R.id.tabLayout)
        val rvList     = view.findViewById<RecyclerView>(R.id.rvList)
        val tvSection  = view.findViewById<TextView>(R.id.tvSectionTitle)
        val btnClear   = view.findViewById<MaterialButton>(R.id.btnClearHistory)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)
        val ivEmptyIcon   = view.findViewById<ImageView>(R.id.ivEmptyIcon)
        val tvEmptyTitle  = view.findViewById<TextView>(R.id.tvEmptyTitle)
        val tvEmptySub    = view.findViewById<TextView>(R.id.tvEmptySubtitle)
        val btnLogout     = view.findViewById<MaterialButton>(R.id.btnLogout)

        rvList.layoutManager = LinearLayoutManager(requireContext())

        val startTab = arguments?.getInt("tab", 0) ?: 0

        tabLayout.addTab(tabLayout.newTab().setText(R.string.profile_tab_history))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.profile_tab_favorites))
        tabLayout.selectTab(tabLayout.getTabAt(startTab))

        fun showEmpty(isHistory: Boolean) {
            emptyState.isVisible = true
            rvList.isVisible     = false
            if (isHistory) {
                ivEmptyIcon.setImageResource(R.drawable.ic_history)
                tvEmptyTitle.text = getString(R.string.profile_history_empty_title)
                tvEmptySub.text   = getString(R.string.profile_history_empty_sub)
            } else {
                ivEmptyIcon.setImageResource(R.drawable.ic_favorite_outline)
                tvEmptyTitle.text = getString(R.string.profile_favorites_empty_title)
                tvEmptySub.text   = getString(R.string.profile_favorites_empty_sub)
            }
        }

        
        val historyAdapter  = HistoryAdapter(
            onDelete = { item -> viewModel.removeHistory(item) },
            onItemClick = { item ->
                mainViewModel.rebuildRouteFromHistory(item)
                (requireActivity() as? MainActivity)
                    ?.showFragment(com.example.indoornavigation.ui.map.MapFragment(), R.id.nav_map)
            }
        )
        val favoritesAdapter = FavoritesAdapter(
            onDelete = { fav  -> viewModel.removeFavorite(fav) },
            onItemClick = { fav ->
                mainViewModel.selectBuildingById(fav.buildingId)
                (requireActivity() as? MainActivity)
                    ?.showFragment(com.example.indoornavigation.ui.map.MapFragment(), R.id.nav_map)
            }
        )

        fun showHistory(list: List<SearchHistoryEntity>) {
            tvSection.text     = getString(R.string.profile_section_history)
            btnClear.isVisible = list.isNotEmpty()
            if (list.isEmpty()) {
                showEmpty(true)
            } else {
                emptyState.isVisible = false
                rvList.isVisible     = true
                if (rvList.adapter !== historyAdapter) rvList.adapter = historyAdapter
                historyAdapter.submitList(list)
            }
        }

        fun showFavorites(list: List<FavoriteEntity>) {
            tvSection.text     = getString(R.string.profile_section_favorites)
            btnClear.isVisible = false
            if (list.isEmpty()) {
                showEmpty(false)
            } else {
                emptyState.isVisible = false
                rvList.isVisible     = true
                if (rvList.adapter !== favoritesAdapter) rvList.adapter = favoritesAdapter
                favoritesAdapter.submitList(list)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchHistory.collect { history ->
                if (tabLayout.selectedTabPosition == 0) showHistory(history)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favorites.collect { favs ->
                if (tabLayout.selectedTabPosition == 1) showFavorites(favs)
            }
        }

        
        when (startTab) {
            0 -> showHistory(viewModel.searchHistory.value)
            1 -> showFavorites(viewModel.favorites.value)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showHistory(viewModel.searchHistory.value)
                    1 -> showFavorites(viewModel.favorites.value)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        btnClear.setOnClickListener {
            viewModel.clearHistory()
            Toast.makeText(requireContext(),
                R.string.profile_history_cleared, Toast.LENGTH_SHORT).show()
        }

        
        btnLogout.setOnClickListener {
            (requireActivity() as? MainActivity)?.onLogout()
        }
    }
}