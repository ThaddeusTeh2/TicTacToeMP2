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
            status = RoomStatus.ACTIVE // directly active for single-device
        )
        val gameState = GameState(
            roomId = roomId,
            movesString = "",
            nextTurnUserId = "X" // use symbol directly
        )
        rooms[roomId] = room
        games[roomId] = gameState
        gameFlows[roomId] = MutableStateFlow(gameState)
        return room
    }

    override suspend fun joinRoom(code: String, userId: String): Room {
        throw UnsupportedOperationException("Join not supported in single-device mode")
    }

    override fun observeGame(roomId: String): Flow<GameState> = gameFlows.getOrPut(roomId) {
        val game = games[roomId] ?: GameState(roomId, "", "X")
        MutableStateFlow(game)
    }

    override suspend fun submitMove(roomId: String, userId: String, cellIndex: Int) {
        val room = rooms[roomId] ?: throw IllegalArgumentException("Room not found")
        val gameState = games[roomId] ?: throw IllegalArgumentException("Game not found")
        if (room.status == RoomStatus.FINISHED) throw IllegalStateException("Game is finished")
        if (cellIndex !in 0..8) throw IllegalArgumentException("Invalid cell index")

        val currentSymbol = gameState.nextTurnUserId // "X" or "O"
        val newMovesString = MoveCodec.append(gameState.movesString, currentSymbol, cellIndex)
        val board = MoveCodec.deriveBoard(newMovesString, room.hostUserId)
        val winner = WinChecker.checkWinner(board)
        val isDraw = WinChecker.isDraw(board)
        val nextSymbol = if (winner != null || isDraw) currentSymbol else if (currentSymbol == "X") "O" else "X"

        val updatedGameState = gameState.copy(
            movesString = newMovesString,
            nextTurnUserId = nextSymbol,
            winnerUserId = if (winner != null) winner.toString() else null,
            updatedAt = System.currentTimeMillis()
        )
        games[roomId] = updatedGameState
        gameFlows[roomId]?.value = updatedGameState
        if (winner != null || isDraw) {
            rooms[roomId] = room.copy(status = RoomStatus.FINISHED, finishedAt = System.currentTimeMillis())
        }
    }

    override suspend fun resetGame(roomId: String) {
        val room = rooms[roomId] ?: throw IllegalArgumentException("Room not found")
        if (room.status != RoomStatus.FINISHED) throw IllegalStateException("Can only reset finished games")
        val reset = GameState(roomId, "", "X", null, System.currentTimeMillis())
        games[roomId] = reset
        gameFlows[roomId]?.value = reset
        rooms[roomId] = room.copy(status = RoomStatus.ACTIVE, finishedAt = null)
    }

    override suspend fun getRoomByCode(code: String): Room? = rooms.values.find { it.code == code }

    private fun generateUniqueCode(): String {
        var code: String
        var attempts = 0
        do {
            code = RoomCodeGenerator.generate(); attempts++
        } while (rooms.values.any { it.code == code } && attempts < 100)
        if (attempts >= 100) throw IllegalStateException("Failed to generate unique room code")
        return code
    }

    fun getRoomById(roomId: String): Room? = rooms[roomId]
}
