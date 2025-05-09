package com.example.frontend_triptales

data class Post(
    val id: Int,
    val title: String,
    val description: String?,
    val createdAt: String,
    val createdBy: User?,
    val image: Image?,
    val likes: List<User>,
    val group: Trip
)
