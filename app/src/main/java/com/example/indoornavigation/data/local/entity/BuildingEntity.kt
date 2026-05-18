package com.example.indoornavigation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buildings_cache")
data class BuildingEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val address: String
)
