package com.dx.tictactoemp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dx.tictactoemp.data.repository.FirebaseGameRepository
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
    val currentUserId: String = "",
    val nextSymbol: Char = 'X',
    val mySymbol: Char? = null,
    val isMyTurn: Boolean = false,
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

    fun observeGame(roomId: String, currentUserId: String = "") {
        viewModelScope.launch {
            // Fetch room data based on repository type
            val room = when (gameRepository) {
                is LocalGameRepository -> gameRepository.getRoomById(roomId)
                is FirebaseGameRepository -> gameRepository.getRoomById(roomId)
                else -> null
            }

            gameRepository.observeGame(roomId).collect { gameState ->
                val hostId = room?.hostUserId ?: ""
                val board = MoveCodec.deriveBoard(gameState.movesString, hostId)
                val winner = WinChecker.checkWinner(board)
                val isDraw = WinChecker.isDraw(board)

                // For Firebase mode: determine user's symbol and turn
                val isFirebaseMode = RepositoryProvider.isFirebaseMode()
                val mySymbol = if (isFirebaseMode && currentUserId.isNotEmpty()) {
                    if (currentUserId == hostId) 'X' else 'O'
                } else null

                val nextSymbol = gameState.nextTurnUserId.firstOrNull() ?: 'X'
                val isMyTurn = if (isFirebaseMode && mySymbol != null) {
                    mySymbol == nextSymbol
                } else {
                    true // In local mode, any tap is allowed
                }

                _uiState.value = GameUiState(
                    room = room,
                    gameState = gameState,
                    board = board,
                    currentUserId = currentUserId,
                    nextSymbol = nextSymbol,
                    mySymbol = mySymbol,
                    isMyTurn = isMyTurn,
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

        // In Firebase mode, use userId; in local mode, use symbol
        val identifier = if (RepositoryProvider.isFirebaseMode()) {
            state.currentUserId
        } else {
            state.nextSymbol.toString()
        }

        viewModelScope.launch {
            try {
                gameRepository.submitMove(roomId, identifier, cellIndex)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("Not your turn", true) == true -> "Not your turn"
                    e.message?.contains("already taken", true) == true -> "Cell already taken"
                    e.message?.contains("finished", true) == true -> "Game is finished"
                    e.message?.contains("Invalid cell", true) == true -> "Invalid cell"
                    e.message?.contains("participant", true) == true -> "You are not a participant"
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
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to reset game")
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
