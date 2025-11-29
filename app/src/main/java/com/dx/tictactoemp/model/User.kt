package com.dx.tictactoemp.model

data class User(
    val id: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

