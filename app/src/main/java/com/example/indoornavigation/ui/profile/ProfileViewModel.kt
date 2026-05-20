package com.example.indoornavigation.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.SessionManager
import com.example.indoornavigation.data.local.entity.FavoriteEntity
import com.example.indoornavigation.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val db  = AppDatabase.getInstance(app)
    val session     = SessionManager(app)

    val searchHistory = db.searchHistoryDao()
        .getByUser(session.userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites = db.favoriteDao()
        .getByUser(session.userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun clearHistory() = viewModelScope.launch {
        db.searchHistoryDao().clearByUser(session.userId)
    }

    fun removeHistory(item: SearchHistoryEntity) = viewModelScope.launch {
        db.searchHistoryDao().delete(item)
    }

    fun removeFavorite(fav: FavoriteEntity) = viewModelScope.launch {
        db.favoriteDao().delete(fav)
    }

    fun addFavorite(building: com.example.indoornavigation.data.model.Building) =
        viewModelScope.launch {
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
        }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>) =
            ProfileViewModel(app) as T
    }
}