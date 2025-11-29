package com.dx.tictactoemp.util

object MoveCodec {
    /**
     * Parse movesString into list of (userId, cellIndex) pairs
     * Example: "user1+0,user2+4" -> [(user1, 0), (user2, 4)]
     */
    fun parse(movesString: String): List<Pair<String, Int>> {
        if (movesString.isEmpty()) return emptyList()

        return movesString.split(",").mapNotNull { move ->
            val parts = move.split("+")
            if (parts.size == 2) {
                val userId = parts[0]
                val cell = parts[1].toIntOrNull()
                if (cell != null && cell in 0..8) {
                    userId to cell
                } else null
            } else null
        }
    }

    /**
     * Append a new move to the movesString
     * Validates that the cell is not already used
     */
    fun append(movesString: String, userId: String, cell: Int): String {
        val moves = parse(movesString)

        // Check if cell already used
        if (moves.any { it.second == cell }) {
            throw IllegalArgumentException("Cell already taken")
        }

        val newMove = "$userId+$cell"
        return if (movesString.isEmpty()) newMove else "$movesString,$newMove"
    }

    /**
     * Derive board from moves
     * Host (first player) = 'X', Joiner = 'O'
     * Returns List<Char?> of size 9
     */
    fun deriveBoard(movesString: String, hostUserId: String): List<Char?> {
        val board = MutableList<Char?>(9) { null }
        val moves = parse(movesString)

        moves.forEach { (userId, cell) ->
            board[cell] = if (userId == hostUserId) 'X' else 'O'
        }

        return board
    }

    /**
     * Get the symbol for a given userId
     */
    fun getSymbol(userId: String, hostUserId: String): Char {
        return if (userId == hostUserId) 'X' else 'O'
    }
}

