package com.example.frontend_triptales

data class User(
    val username: String,
    val email: String,
    val password: String,
    val bio: String,
    val avatar: String? = null
)
