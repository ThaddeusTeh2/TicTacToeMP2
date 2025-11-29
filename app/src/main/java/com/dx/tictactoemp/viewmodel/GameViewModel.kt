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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GameUiState(
    val room: Room? = null,
    val gameState: GameState? = null,
    val board: List<Char?> = List(9) { null },
    val currentUserId: String = "",
    val isMyTurn: Boolean = false,
    val mySymbol: Char? = null,
    val currentTurnSymbol: Char? = null,
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

    fun observeGame(roomId: String, currentUserId: String) {
        viewModelScope.launch {
            // Get room details
            val room = (gameRepository as? LocalGameRepository)?.getRoomById(roomId)

            gameRepository.observeGame(roomId).collect { gameState ->
                val board = MoveCodec.deriveBoard(gameState.movesString, room?.hostUserId ?: "")
                val winner = WinChecker.checkWinner(board)
                val isDraw = WinChecker.isDraw(board)
                val isMyTurn = gameState.nextTurnUserId == currentUserId
                val mySymbol = if (room != null) MoveCodec.getSymbol(currentUserId, room.hostUserId) else null
                val currentTurnSymbol = if (room != null) MoveCodec.getSymbol(gameState.nextTurnUserId, room.hostUserId) else null

                _uiState.value = GameUiState(
                    room = room,
                    gameState = gameState,
                    board = board,
                    currentUserId = currentUserId,
                    isMyTurn = isMyTurn,
                    mySymbol = mySymbol,
                    currentTurnSymbol = currentTurnSymbol,
                    winnerSymbol = winner,
                    isDraw = isDraw,
                    isFinished = winner != null || isDraw,
                    error = null
                )
            }
        }
    }

    fun submitMove(cellIndex: Int) {
        val roomId = _uiState.value.room?.id ?: return
        val userId = _uiState.value.currentUserId

        viewModelScope.launch {
            try {
                gameRepository.submitMove(roomId, userId, cellIndex)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("Not your turn") == true -> "Not your turn"
                    e.message?.contains("already taken") == true -> "Cell already taken"
                    e.message?.contains("finished") == true -> "Game is finished"
                    else -> e.message ?: "Failed to submit move"
                }
                _uiState.value = _uiState.value.copy(error = errorMessage)
            }
        }
    }

    fun rematch() {
        val roomId = _uiState.value.room?.id ?: return

        viewModelScope.launch {
            try {
                gameRepository.resetGame(roomId)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to reset game"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

