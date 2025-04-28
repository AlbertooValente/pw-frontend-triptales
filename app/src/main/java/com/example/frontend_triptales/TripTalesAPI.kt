package com.example.frontend_triptales

import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface TripTalesApi {
    /*

    @GET("offers/")
    suspend fun getOffers(): List<Offer>

     */
}

object RetrofitInstance {
    val api: TripTalesApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TripTalesApi::class.java)
    }
}