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
                val symbol = parts[0] // now expected to be "X" or "O"
                val cell = parts[1].toIntOrNull()
                if (cell != null && cell in 0..8) symbol to cell else null
            } else null
        }
    }

    /**
     * Append a new move to the movesString
     * Validates that the cell is not already used
     */
    fun append(movesString: String, symbol: String, cell: Int): String {
        val moves = parse(movesString)

        // Check if cell already used
        if (moves.any { it.second == cell }) {
            throw IllegalArgumentException("Cell already taken")
        }

        val newMove = "$symbol+$cell"
        return if (movesString.isEmpty()) newMove else "$movesString,$newMove"
    }

    /**
     * Derive board from moves
     * Host (first player) = 'X', Joiner = 'O'
     * Returns List<Char?> of size 9
     */
    fun deriveBoard(movesString: String, @Suppress("UNUSED_PARAMETER") hostUserId: String): List<Char?> {
        val board = MutableList<Char?>(9) { null }
        parse(movesString).forEach { (symbol, cell) ->
            board[cell] = if (symbol == "X") 'X' else 'O'
        }
        return board
    }

    /**
     * Get the symbol for a given userId
     */
    fun getSymbol(userId: String, hostUserId: String): Char {
        return if (userId == "X") 'X' else if (userId == "O") 'O' else 'X'
    }

    fun nextSymbol(movesString: String): Char = if (parse(movesString).size % 2 == 0) 'X' else 'O'
}
