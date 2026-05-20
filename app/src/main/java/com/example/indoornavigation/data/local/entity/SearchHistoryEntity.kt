package com.example.indoornavigation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val fromRoomName: String,
    val fromRoomNameEn: String?,
    val toRoomName: String,
    val toRoomNameEn: String?,
    val buildingName: String,
    val buildingNameEn: String?,
    val timestamp: Long = System.currentTimeMillis()
)