package com.example.frontend_triptales

data class Image(
    val id: Int,
    val image: String,
    val description: String?,
    val createdAt: String,
    val createdBy: User?,
    val latitude: Double?,
    val longitude: Double?
)
