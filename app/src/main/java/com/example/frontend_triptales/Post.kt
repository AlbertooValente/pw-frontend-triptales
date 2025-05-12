package com.example.frontend_triptales

data class Post(
    val id: Int,
    val title: String,
    val description: String?,
    val created_at: String,
    val created_by: Int,
    val image: Int,
    val likes: List<Int>,
    val group: Int
)
