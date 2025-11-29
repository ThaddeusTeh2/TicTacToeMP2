package com.dx.tictactoemp.data.repository

import com.dx.tictactoemp.model.GameState
import com.dx.tictactoemp.model.Room
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun createRoom(hostUserId: String): Room
    suspend fun joinRoom(code: String, userId: String): Room
    fun observeGame(roomId: String): Flow<GameState>
    suspend fun submitMove(roomId: String, userId: String, cellIndex: Int)
    suspend fun resetGame(roomId: String)
    suspend fun getRoomByCode(code: String): Room?
}

