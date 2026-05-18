package com.example.indoornavigation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pois_cache")
data class PoiEntity(
    @PrimaryKey val id: Int,
    val floorId: Int,
    val x: Float,
    val y: Float,
    val name: String,
    val type: String
)
