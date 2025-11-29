package com.dx.tictactoemp.util

object WinChecker {
    // All possible winning combinations (rows, columns, diagonals)
    private val winningLines = listOf(
        listOf(0, 1, 2), // Top row
        listOf(3, 4, 5), // Middle row
        listOf(6, 7, 8), // Bottom row
        listOf(0, 3, 6), // Left column
        listOf(1, 4, 7), // Middle column
        listOf(2, 5, 8), // Right column
        listOf(0, 4, 8), // Diagonal \
        listOf(2, 4, 6)  // Diagonal /
    )

    /**
     * Check if there's a winner on the board
     * @param board List of 9 cells (null = empty, 'X' or 'O')
     * @return 'X' or 'O' if winner found, null otherwise
     */
    fun checkWinner(board: List<Char?>): Char? {
        for (line in winningLines) {
            val (a, b, c) = line
            if (board[a] != null && board[a] == board[b] && board[a] == board[c]) {
                return board[a]
            }
        }
        return null
    }

    /**
     * Check if the game is a draw (board full with no winner)
     */
    fun isDraw(board: List<Char?>): Boolean {
        return board.all { it != null } && checkWinner(board) == null
    }
}

