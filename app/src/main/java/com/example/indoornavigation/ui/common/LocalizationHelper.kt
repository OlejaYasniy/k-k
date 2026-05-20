package com.example.indoornavigation.ui.common

import android.content.Context
import com.example.indoornavigation.data.model.*
import com.example.indoornavigation.data.local.entity.SearchHistoryEntity
import com.example.indoornavigation.data.local.entity.FavoriteEntity

object LocalizationHelper {
    private fun isEn(context: Context): Boolean {
        return context.resources.configuration.locales[0].language == "en"
    }

    private fun localize(ru: String, en: String?, context: Context): String {
        return if (isEn(context) && !en.isNullOrBlank()) en else ru
    }

    fun localizeName(building: Building, context: Context): String = localize(building.name, building.nameEn, context)
    fun localizeAddress(building: Building, context: Context): String = localize(building.address, building.addressEn, context)
    
    fun localizeName(floor: Floor, context: Context): String = localize(floor.name, floor.nameEn, context)
    fun localizeName(room: Room, context: Context): String = localize(room.name, room.nameEn, context)
    fun localizeName(poi: Poi, context: Context): String = localize(poi.name, poi.nameEn, context)
    
    fun localizeHistoryFrom(history: SearchHistoryEntity, context: Context): String = localize(history.fromRoomName, history.fromRoomNameEn, context)
    fun localizeHistoryTo(history: SearchHistoryEntity, context: Context): String = localize(history.toRoomName, history.toRoomNameEn, context)

    fun localizeAddress(fav: FavoriteEntity, context: Context): String = localize(fav.buildingAddress, fav.buildingAddressEn, context)
    fun localizeName(fav: FavoriteEntity, context: Context): String = localize(fav.buildingName, fav.buildingNameEn, context)

    // Legacy fallback for UI components that only have Strings
    fun localizeName(name: String, context: Context): String = name
    fun localizeAddress(address: String, context: Context): String = address
}
