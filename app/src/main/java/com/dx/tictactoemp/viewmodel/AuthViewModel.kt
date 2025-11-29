package com.dx.tictactoemp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dx.tictactoemp.data.repository.AuthRepository
import com.dx.tictactoemp.di.RepositoryProvider
import com.dx.tictactoemp.model.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AuthViewModel(
    private val authRepository: AuthRepository = RepositoryProvider.provideAuthRepository()
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUserFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Initialize with a guest user
        viewModelScope.launch {
            if (authRepository.getCurrentUser() == null) {
                val guestUser = User(
                    id = UUID.randomUUID().toString(),
                    displayName = "Guest${(1000..9999).random()}"
                )
                authRepository.setCurrentUser(guestUser)
            }
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                val updatedUser = user.copy(displayName = name)
                authRepository.setCurrentUser(updatedUser)
            }
        }
    }
}

