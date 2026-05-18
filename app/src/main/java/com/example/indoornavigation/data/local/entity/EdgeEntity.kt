package com.example.indoornavigation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "edges_cache")
data class EdgeEntity(
    @PrimaryKey val edgeKey: String, 
    val floorId: Int,
    val fromNodeId: Int,
    val toNodeId: Int,
    val weight: Float
)
