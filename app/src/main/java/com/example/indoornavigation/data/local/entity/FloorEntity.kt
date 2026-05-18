package com.example.indoornavigation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "floors_cache")
data class FloorEntity(
    @PrimaryKey val id: Int,
    val buildingId: Int,
    val name: String,
    val level: Int
)
