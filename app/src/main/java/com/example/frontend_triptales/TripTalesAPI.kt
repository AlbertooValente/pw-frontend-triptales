package com.example.frontend_triptales

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class LoginRequest(val username: String, val password: String)
data class TokenResponse(val token: String)

interface TripTalesApi {
    @Multipart
    @POST("users/register/")
    suspend fun register(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("bio") bio: RequestBody,
        @Part avatar: MultipartBody.Part?
    ): Response<TokenResponse>

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


//salvo l'ip del server come variabile globale
object Constants {
    const val BASE_URL = "http://192.168.1.6:8000"  //da sostituire ogni volta con l'ip del backend
}

object RetrofitInstance {
    val api: TripTalesApi by lazy {
        Retrofit.Builder()
            .baseUrl("${Constants.BASE_URL}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TripTalesApi::class.java)
    }
}