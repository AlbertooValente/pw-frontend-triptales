package com.example.frontend_triptales

data class Trip(
    val id: Int,
    val name: String,
    val description: String,
    val members: List<User>
)
