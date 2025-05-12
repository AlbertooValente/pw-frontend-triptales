package com.example.frontend_triptales

data class Image(
    val id: Int,
    val image: String,
    val description: String?,
    val created_at: String,
    val created_by: Int,
    val latitude: Double,
    val longitude: Double
)
