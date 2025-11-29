package com.dx.tictactoemp.model

data class GameState(
    val roomId: String,
    val movesString: String = "",
    val nextTurnUserId: String,
    val winnerUserId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

