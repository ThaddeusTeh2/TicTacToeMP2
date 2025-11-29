package com.dx.tictactoemp.data.repository

import com.dx.tictactoemp.model.GameState
import com.dx.tictactoemp.model.Room
import com.dx.tictactoemp.model.RoomStatus
import com.dx.tictactoemp.util.MoveCodec
import com.dx.tictactoemp.util.RoomCodeGenerator
import com.dx.tictactoemp.util.WinChecker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseGameRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : GameRepository {

    override suspend fun createRoom(hostUserId: String): Room {
        val roomId = UUID.randomUUID().toString()
        var code: String
        var attempts = 0

        // Generate unique 4-digit code with retry
        do {
            code = RoomCodeGenerator.generate()
            val existingRooms = firestore.collection("rooms")
                .whereEqualTo("code", code)
                .get()
                .await()

            if (existingRooms.isEmpty) break
            attempts++
        } while (attempts < 5)

        if (attempts >= 5) {
            throw IllegalStateException("Failed to generate unique room code after 5 attempts")
        }

        val room = Room(
            id = roomId,
            code = code,
            hostUserId = hostUserId,
            participantUserIds = listOf(hostUserId),
            status = RoomStatus.WAITING,
            createdAt = System.currentTimeMillis()
        )

        // Write room document
        val roomData = hashMapOf(
            "code" to code,
            "hostUserId" to hostUserId,
            "participantUserIds" to listOf(hostUserId),
            "status" to "waiting",
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("rooms").document(roomId).set(roomData).await()

        // Write initial game state
        val gameData = hashMapOf(
            "movesString" to "",
            "nextTurnSymbol" to "X",
            "winnerSymbol" to null,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("games").document(roomId).set(gameData).await()

        return room
    }

    override suspend fun joinRoom(code: String, userId: String): Room {
        // Query room by code
        val roomsSnapshot = firestore.collection("rooms")
            .whereEqualTo("code", code)
            .get()
            .await()

        if (roomsSnapshot.isEmpty) {
            throw IllegalArgumentException("Room not found")
        }

        val roomDoc = roomsSnapshot.documents[0]
        val roomId = roomDoc.id
        val status = roomDoc.getString("status") ?: "waiting"
        val participants = roomDoc.get("participantUserIds") as? List<*> ?: emptyList<String>()

        if (status != "waiting") {
            throw IllegalStateException("Room is not accepting players")
        }

        if (participants.size >= 2) {
            throw IllegalStateException("Room is full")
        }

        if (participants.contains(userId)) {
            throw IllegalStateException("Already in room")
        }

        // Update room: add participant and set status to active
        firestore.collection("rooms").document(roomId).update(
            mapOf(
                "participantUserIds" to FieldValue.arrayUnion(userId),
                "status" to "active"
            )
        ).await()

        // Fetch updated room
        val updatedDoc = firestore.collection("rooms").document(roomId).get().await()
        return documentToRoom(roomId, updatedDoc.data ?: emptyMap())
    }

    override fun observeGame(roomId: String): Flow<GameState> = callbackFlow {
        val listener = firestore.collection("games").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val movesString = snapshot.getString("movesString") ?: ""
                    val nextTurnSymbol = snapshot.getString("nextTurnSymbol") ?: "X"
                    val winnerSymbol = snapshot.getString("winnerSymbol")
                    val updatedAt = snapshot.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()

                    val gameState = GameState(
                        roomId = roomId,
                        movesString = movesString,
                        nextTurnUserId = nextTurnSymbol,
                        winnerUserId = winnerSymbol,
                        updatedAt = updatedAt
                    )
                    trySend(gameState)
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun submitMove(roomId: String, userId: String, cellIndex: Int) {
        firestore.runTransaction { transaction ->
            val roomRef = firestore.collection("rooms").document(roomId)
            val gameRef = firestore.collection("games").document(roomId)

            val roomSnapshot = transaction.get(roomRef)
            val gameSnapshot = transaction.get(gameRef)

            val participants = roomSnapshot.get("participantUserIds") as? List<*> ?: emptyList<String>()
            val hostUserId = roomSnapshot.getString("hostUserId") ?: ""
            val status = roomSnapshot.getString("status") ?: "waiting"

            // Validate participant
            if (!participants.contains(userId)) {
                throw IllegalStateException("User is not a participant")
            }

            // Validate game not finished
            if (status == "finished") {
                throw IllegalStateException("Game is finished")
            }

            val movesString = gameSnapshot.getString("movesString") ?: ""
            val nextTurnSymbol = gameSnapshot.getString("nextTurnSymbol") ?: "X"

            // Determine user's role (host = X, joiner = O)
            val userSymbol = if (userId == hostUserId) "X" else "O"

            // Validate it's the user's turn
            if (userSymbol != nextTurnSymbol) {
                throw IllegalStateException("Not your turn")
            }

            // Validate cell is in range
            if (cellIndex !in 0..8) {
                throw IllegalArgumentException("Invalid cell index")
            }

            // Append move (will throw if cell already taken)
            val newMovesString = MoveCodec.append(movesString, userSymbol, cellIndex)

            // Derive board and check for winner/draw
            val board = MoveCodec.deriveBoard(newMovesString, hostUserId)
            val winner = WinChecker.checkWinner(board)
            val isDraw = WinChecker.isDraw(board)

            // Determine next turn symbol
            val newNextTurnSymbol = if (winner != null || isDraw) {
                nextTurnSymbol
            } else {
                if (nextTurnSymbol == "X") "O" else "X"
            }

            // Update game state
            val gameUpdates = hashMapOf<String, Any?>(
                "movesString" to newMovesString,
                "nextTurnSymbol" to newNextTurnSymbol,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (winner != null) {
                gameUpdates["winnerSymbol"] = winner.toString()
            }

            transaction.update(gameRef, gameUpdates)

            // Update room status if game finished
            if (winner != null || isDraw) {
                transaction.update(
                    roomRef,
                    mapOf(
                        "status" to "finished",
                        "finishedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
    }

    override suspend fun resetGame(roomId: String) {
        firestore.runTransaction { transaction ->
            val roomRef = firestore.collection("rooms").document(roomId)
            val gameRef = firestore.collection("games").document(roomId)

            val roomSnapshot = transaction.get(roomRef)
            val status = roomSnapshot.getString("status") ?: "active"

            if (status != "finished") {
                throw IllegalStateException("Can only reset finished games")
            }

            // Reset game state
            transaction.update(
                gameRef,
                mapOf(
                    "movesString" to "",
                    "nextTurnSymbol" to "X",
                    "winnerSymbol" to null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Update room to active
            transaction.update(
                roomRef,
                mapOf(
                    "status" to "active",
                    "finishedAt" to null
                )
            )
        }.await()
    }

    override suspend fun getRoomByCode(code: String): Room? {
        val roomsSnapshot = firestore.collection("rooms")
            .whereEqualTo("code", code)
            .get()
            .await()

        if (roomsSnapshot.isEmpty) return null

        val doc = roomsSnapshot.documents[0]
        return documentToRoom(doc.id, doc.data ?: emptyMap())
    }

    suspend fun getRoomById(roomId: String): Room? {
        val doc = firestore.collection("rooms").document(roomId).get().await()
        if (!doc.exists()) return null
        return documentToRoom(roomId, doc.data ?: emptyMap())
    }

    private fun documentToRoom(id: String, data: Map<String, Any>): Room {
        val code = data["code"] as? String ?: ""
        val hostUserId = data["hostUserId"] as? String ?: ""
        val participantUserIds = (data["participantUserIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val statusStr = data["status"] as? String ?: "waiting"
        val status = when (statusStr) {
            "waiting" -> RoomStatus.WAITING
            "active" -> RoomStatus.ACTIVE
            "finished" -> RoomStatus.FINISHED
            else -> RoomStatus.WAITING
        }
        val createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        val finishedAt = (data["finishedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time

        return Room(
            id = id,
            code = code,
            hostUserId = hostUserId,
            participantUserIds = participantUserIds,
            status = status,
            createdAt = createdAt,
            finishedAt = finishedAt
        )
    }
}

