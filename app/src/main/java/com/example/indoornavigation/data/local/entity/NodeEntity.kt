package com.example.indoornavigation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nodes_cache")
data class NodeEntity(
    @PrimaryKey val id: Int,
    val floorId: Int,
    val x: Float,
    val y: Float,
    val type: String = "normal"
)
