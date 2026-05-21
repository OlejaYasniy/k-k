package com.example.indoornavigation.data.model



data class Building(
    val id: Int,
    val name: String,
    val nameEn: String?,
    val address: String,
    val addressEn: String?
)

data class Floor(
    val id: Int,
    val buildingId: Int,
    val name: String,
    val nameEn: String?,
    val level: Int
)

data class Room(
    val id: Int,
    val floorId: Int,
    val name: String,
    val nameEn: String?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class Node(
    val id: Int,
    val floorId: Int,
    val x: Float,
    val y: Float,
    val type: String = "normal"
)

data class Edge(
    val from: Int,
    val to: Int,
    val weight: Float
)

data class Poi(
    val id: Int,
    val floorId: Int,
    val x: Float,
    val y: Float,
    val name: String,
    val nameEn: String?,
    val type: String
)

data class CrossFloorEdge(
    val fromNodeId: Int,
    val fromFloorId: Int,
    val toNodeId: Int,
    val toFloorId: Int,
    val type: String,
    val weight: Float
)

data class Step(
    val type: String,
    val text: String,
    val floorId: Int? = null
)

data class RouteResult(
    val fromRoom: Room,
    val toRoom: Room,
    val path: List<Node>,
    val steps: List<Step>,
    val lengthMeters: Float
)



data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class VerifyRequest(
    val username: String,
    val email: String,
    val password: String,
    val code: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String
)

data class BasicResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)