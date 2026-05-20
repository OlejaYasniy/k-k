package com.example.indoornavigation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms_cache")
data class RoomCacheEntity(
    @PrimaryKey val id: Int,
    val floorId: Int,
    val name: String,
    val nameEn: String?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)
