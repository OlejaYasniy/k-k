package com.example.indoornavigation.data.remote

import com.example.indoornavigation.data.model.*
import retrofit2.http.*

interface ApiService {

    

    @POST("register/send-code")
    suspend fun registerSendCode(@Body body: RegisterRequest): BasicResponse

    @POST("register/verify")
    suspend fun registerVerify(@Body body: VerifyRequest): UserResponse

    @POST("register")
    suspend fun registerDirect(@Body body: RegisterRequest): UserResponse

    @POST("login")
    suspend fun login(@Body body: LoginRequest): UserResponse

    

    @GET("buildings")
    suspend fun getBuildings(): List<Building>

    @GET("buildings/{buildingId}/floors")
    suspend fun getFloors(@Path("buildingId") buildingId: Int): List<Floor>

    

    @GET("floors/{floorId}/rooms")
    suspend fun getRooms(@Path("floorId") floorId: Int): List<Room>

    @GET("floors/{floorId}/nodes")
    suspend fun getNodes(@Path("floorId") floorId: Int): List<Node>

    @GET("floors/{floorId}/edges")
    suspend fun getEdges(@Path("floorId") floorId: Int): List<Edge>

    @GET("floors/{floorId}/pois")
    suspend fun getPois(@Path("floorId") floorId: Int): List<Poi>
}