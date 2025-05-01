package com.example.frontend_triptales

import retrofit2.Response
import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class LoginRequest(val username: String, val password: String)
data class TokenResponse(val token: String)

interface TripTalesApi {
    @POST("users/register/")
    suspend fun register(@Body user: User): Response<TokenResponse>

    @POST("users/login/")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @GET("users/profile/")
    suspend fun getUser(@Header("Authorization") token: String): Response<User>

    @PATCH("users/profile/update/")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Body updates: Map<String, String>
    ): Response<User>
}

object RetrofitInstance {
    val api: TripTalesApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.3:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TripTalesApi::class.java)
    }
}