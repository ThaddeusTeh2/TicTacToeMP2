package com.dx.tictactoemp.data.repository

import com.dx.tictactoemp.model.GameState
import com.dx.tictactoemp.model.Room
import com.dx.tictactoemp.model.RoomStatus
import com.dx.tictactoemp.util.MoveCodec
import com.dx.tictactoemp.util.RoomCodeGenerator
import com.dx.tictactoemp.util.WinChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class LocalGameRepository : GameRepository {
    private val rooms = mutableMapOf<String, Room>()
    private val games = mutableMapOf<String, GameState>()
    private val gameFlows = mutableMapOf<String, MutableStateFlow<GameState>>()

    override suspend fun createRoom(hostUserId: String): Room {
        val roomId = UUID.randomUUID().toString()
        val code = generateUniqueCode()

        val room = Room(
            id = roomId,
            code = code,
            hostUserId = hostUserId,
            participantUserIds = listOf(hostUserId),
            status = RoomStatus.WAITING
        )

        val gameState = GameState(
            roomId = roomId,
            movesString = "",
            nextTurnUserId = hostUserId
        )

        rooms[roomId] = room
        games[roomId] = gameState
        gameFlows[roomId] = MutableStateFlow(gameState)

        return room
    }

    override suspend fun joinRoom(code: String, userId: String): Room {
        val room = rooms.values.find { it.code == code }
            ?: throw IllegalArgumentException("Room not found")

        if (room.status != RoomStatus.WAITING) {
            throw IllegalStateException("Room is not accepting players")
        }

        if (room.participantUserIds.size >= 2) {
            throw IllegalStateException("Room is full")
        }

        if (room.participantUserIds.contains(userId)) {
            throw IllegalStateException("Already in room")
        }

        val updatedRoom = room.copy(
            participantUserIds = room.participantUserIds + userId,
            status = RoomStatus.ACTIVE
        )

        rooms[room.id] = updatedRoom
        return updatedRoom
    }

    override fun observeGame(roomId: String): Flow<GameState> {
        return gameFlows.getOrPut(roomId) {
            val game = games[roomId] ?: GameState(
                roomId = roomId,
                movesString = "",
                nextTurnUserId = ""
            )
            MutableStateFlow(game)
        }
    }

    override suspend fun submitMove(roomId: String, userId: String, cellIndex: Int) {
        val room = rooms[roomId] ?: throw IllegalArgumentException("Room not found")
        val gameState = games[roomId] ?: throw IllegalArgumentException("Game not found")

        // Check if game is finished
        if (room.status == RoomStatus.FINISHED) {
            throw IllegalStateException("Game is finished")
        }

        // Validate turn
        if (gameState.nextTurnUserId != userId) {
            throw IllegalStateException("Not your turn")
        }

        // Validate cell is in range
        if (cellIndex !in 0..8) {
            throw IllegalArgumentException("Invalid cell index")
        }

        // Append move (will throw if cell already taken)
        val newMovesString = MoveCodec.append(gameState.movesString, userId, cellIndex)

        // Derive board and check for winner/draw
        val board = MoveCodec.deriveBoard(newMovesString, room.hostUserId)
        val winner = WinChecker.checkWinner(board)
        val isDraw = WinChecker.isDraw(board)

        // Determine next turn user
        val nextUserId = if (winner != null || isDraw) {
            gameState.nextTurnUserId // Keep same if game ended
        } else {
            // Switch to other player
            room.participantUserIds.first { it != userId }
        }

        // Determine winner user ID
        val winnerUserId = if (winner != null) {
            // Find which user has the winning symbol
            if (MoveCodec.getSymbol(room.hostUserId, room.hostUserId) == winner) {
                room.hostUserId
            } else {
                room.participantUserIds.first { it != room.hostUserId }
            }
        } else null

        // Update game state
        val updatedGameState = gameState.copy(
            movesString = newMovesString,
            nextTurnUserId = nextUserId,
            winnerUserId = winnerUserId,
            updatedAt = System.currentTimeMillis()
        )

        games[roomId] = updatedGameState
        gameFlows[roomId]?.value = updatedGameState

        // Update room status if game finished
        if (winner != null || isDraw) {
            val updatedRoom = room.copy(
                status = RoomStatus.FINISHED,
                finishedAt = System.currentTimeMillis()
            )
            rooms[roomId] = updatedRoom
        }
    }

    override suspend fun resetGame(roomId: String) {
        val room = rooms[roomId] ?: throw IllegalArgumentException("Room not found")

        if (room.status != RoomStatus.FINISHED) {
            throw IllegalStateException("Can only reset finished games")
        }

        // Reset game state
        val resetGameState = GameState(
            roomId = roomId,
            movesString = "",
            nextTurnUserId = room.hostUserId,
            winnerUserId = null,
            updatedAt = System.currentTimeMillis()
        )

        games[roomId] = resetGameState
        gameFlows[roomId]?.value = resetGameState

        // Update room to ACTIVE (or WAITING if only host)
        val newStatus = if (room.participantUserIds.size >= 2) RoomStatus.ACTIVE else RoomStatus.WAITING
        val updatedRoom = room.copy(
            status = newStatus,
            finishedAt = null
        )
        rooms[roomId] = updatedRoom
    }

    override suspend fun getRoomByCode(code: String): Room? {
        return rooms.values.find { it.code == code }
    }

    private fun generateUniqueCode(): String {
        var code: String
        var attempts = 0
        do {
            code = RoomCodeGenerator.generate()
            attempts++
        } while (rooms.values.any { it.code == code } && attempts < 100)

        if (attempts >= 100) {
            throw IllegalStateException("Failed to generate unique room code")
        }

        return code
    }

    // Helper for Sprint 1 testing
    fun getRoomById(roomId: String): Room? = rooms[roomId]
}

