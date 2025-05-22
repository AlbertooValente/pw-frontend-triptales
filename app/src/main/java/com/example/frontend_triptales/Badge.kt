package com.example.frontend_triptales

data class Badge(
    val id: Int,
    val name: String,
    val description: String
)

data class UserWithBadge(
    val userId: Int,
    val username: String,
    val badge: Badge
)
