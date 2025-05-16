package com.example.frontend_triptales

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val password: String,
    val bio: String? = null,
    val avatar: String? = null
)
