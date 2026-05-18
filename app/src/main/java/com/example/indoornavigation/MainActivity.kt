package com.example.indoornavigation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.SessionManager
import com.example.indoornavigation.data.local.SettingsManager
import com.example.indoornavigation.data.remote.RetrofitProvider
import com.example.indoornavigation.data.repository.NavigationRepository
import com.example.indoornavigation.ui.auth.AuthViewModel
import com.example.indoornavigation.ui.auth.LoginFragment
import com.example.indoornavigation.ui.main.MainViewModel
import com.example.indoornavigation.ui.map.MapFragment
import com.example.indoornavigation.ui.profile.ProfileFragment
import com.example.indoornavigation.ui.settings.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            application,
            NavigationRepository(
                api     = RetrofitProvider.api,
                db      = AppDatabase.getInstance(this),
                context = applicationContext
            )
        )
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(application)
    }

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var session: SessionManager

    
    private var skipNextNavigation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        
        SettingsManager(this).applyTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session   = SessionManager(this)
        bottomNav = findViewById(R.id.bottomNav)

        if (viewModel.selectedBuilding.value == null) {
            viewModel.loadBuildings()
        } else {
            viewModel.refreshSelectedData()
        }

        
        bottomNav.setOnItemSelectedListener { item ->
            if (skipNextNavigation) {
                skipNextNavigation = false
                return@setOnItemSelectedListener true
            }
            when (item.itemId) {
                R.id.nav_profile -> {
                    if (session.isLoggedIn) replaceFragment(ProfileFragment())
                    else replaceFragment(LoginFragment())
                    true
                }
                R.id.nav_map      -> { replaceFragment(MapFragment());      true }
                R.id.nav_settings -> { replaceFragment(SettingsFragment()); true }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            
            replaceFragment(MapFragment())
            skipNextNavigation = true
            bottomNav.selectedItemId = R.id.nav_map
        }
        
        
    }

    fun showFragment(fragment: Fragment, bottomNavItemId: Int) {
        replaceFragment(fragment)
        skipNextNavigation = true
        bottomNav.selectedItemId = bottomNavItemId
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    
    fun onAuthSuccess() {
        showFragment(ProfileFragment(), R.id.nav_profile)
    }

    
    fun onLogout() {
        authViewModel.logout()
        showFragment(MapFragment(), R.id.nav_map)
    }
}