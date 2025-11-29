package com.dx.tictactoemp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dx.tictactoemp.data.repository.GameRepository
import com.dx.tictactoemp.di.RepositoryProvider
import com.dx.tictactoemp.model.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RoomsUiState(
    val createdRoom: Room? = null,
    val joinedRoom: Room? = null,
    val error: String? = null,
    val isLoading: Boolean = false
)

class RoomsViewModel(
    private val gameRepository: GameRepository = RepositoryProvider.provideGameRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomsUiState())
    val uiState: StateFlow<RoomsUiState> = _uiState

    fun createRoom(hostUserId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val room = gameRepository.createRoom(hostUserId)
                _uiState.value = _uiState.value.copy(
                    createdRoom = room,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create room",
                    isLoading = false
                )
            }
        }
    }

    fun joinRoom(code: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val room = gameRepository.joinRoom(code, userId)
                _uiState.value = _uiState.value.copy(
                    joinedRoom = room,
                    isLoading = false
                )
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("not found") == true -> "Room not found"
                    e.message?.contains("full") == true -> "Room is full"
                    e.message?.contains("not accepting") == true -> "Room is not accepting players"
                    else -> e.message ?: "Failed to join room"
                }
                _uiState.value = _uiState.value.copy(
                    error = errorMessage,
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearCreatedRoom() {
        _uiState.value = _uiState.value.copy(createdRoom = null)
    }

    fun clearJoinedRoom() {
        _uiState.value = _uiState.value.copy(joinedRoom = null)
    }
}

