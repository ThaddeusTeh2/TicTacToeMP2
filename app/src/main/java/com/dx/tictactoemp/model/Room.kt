package com.dx.tictactoemp.model

enum class RoomStatus {
    WAITING,
    ACTIVE,
    FINISHED
}

data class Room(
    val id: String,
    val code: String,
    val hostUserId: String,
    val participantUserIds: List<String>,
    val status: RoomStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

