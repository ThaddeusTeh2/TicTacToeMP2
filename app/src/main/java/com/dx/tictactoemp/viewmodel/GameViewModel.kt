package com.dx.tictactoemp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dx.tictactoemp.data.repository.GameRepository
import com.dx.tictactoemp.data.repository.LocalGameRepository
import com.dx.tictactoemp.di.RepositoryProvider
import com.dx.tictactoemp.model.GameState
import com.dx.tictactoemp.model.Room
import com.dx.tictactoemp.util.MoveCodec
import com.dx.tictactoemp.util.WinChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val room: Room? = null,
    val gameState: GameState? = null,
    val board: List<Char?> = List(9) { null },
    val nextSymbol: Char = 'X',
    val winnerSymbol: Char? = null,
    val isDraw: Boolean = false,
    val isFinished: Boolean = false,
    val error: String? = null
)

class GameViewModel(
    private val gameRepository: GameRepository = RepositoryProvider.provideGameRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    fun observeGame(roomId: String) {
        viewModelScope.launch {
            val room = (gameRepository as? LocalGameRepository)?.getRoomById(roomId)
            gameRepository.observeGame(roomId).collect { gameState ->
                val board = MoveCodec.deriveBoard(gameState.movesString, room?.hostUserId ?: "")
                val winner = WinChecker.checkWinner(board)
                val isDraw = WinChecker.isDraw(board)
                val nextSymbol = if (winner != null || isDraw) gameState.nextTurnUserId.first() else MoveCodec.nextSymbol(gameState.movesString)
                _uiState.value = GameUiState(
                    room = room,
                    gameState = gameState,
                    board = board,
                    nextSymbol = nextSymbol,
                    winnerSymbol = winner,
                    isDraw = isDraw,
                    isFinished = winner != null || isDraw,
                    error = null
                )
            }
        }
    }

    fun submitMove(cellIndex: Int) {
        val state = _uiState.value
        val roomId = state.room?.id ?: return
        val symbol = state.nextSymbol.toString()
        viewModelScope.launch {
            try {
                gameRepository.submitMove(roomId, symbol, cellIndex)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("already taken", true) == true -> "Cell already taken"
                    e.message?.contains("finished", true) == true -> "Game is finished"
                    e.message?.contains("Invalid cell", true) == true -> "Invalid cell"
                    else -> e.message ?: "Move failed"
                }
                _uiState.value = _uiState.value.copy(error = msg)
            }
        }
    }

    fun rematch() {
        val roomId = _uiState.value.room?.id ?: return
        viewModelScope.launch {
            try {
                gameRepository.resetGame(roomId)
                _uiState.value = _uiState.value.copy(error = "Rematch started")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to reset game")
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
