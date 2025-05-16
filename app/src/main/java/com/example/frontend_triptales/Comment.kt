package com.example.frontend_triptales

data class Comment(
    val id: Int,
    val post: Int,
    val author: Int,
    val text: String,
    val created_at: String
)
