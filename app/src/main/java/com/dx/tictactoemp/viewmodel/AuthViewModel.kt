package com.dx.tictactoemp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dx.tictactoemp.data.repository.AuthRepository
import com.dx.tictactoemp.data.repository.FirebaseAuthRepository
import com.dx.tictactoemp.di.RepositoryProvider
import com.dx.tictactoemp.model.User
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Only initialize guest user if in local mode
        if (!RepositoryProvider.isFirebaseMode()) {
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
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null

            val firebaseRepo = RepositoryProvider.provideFirebaseAuthRepository()
            if (firebaseRepo == null) {
                _authError.value = "Firebase auth not available"
                _isLoading.value = false
                return@launch
            }

            val result = firebaseRepo.signInWithGoogle()
            result.fold(
                onSuccess = {
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _authError.value = error.message ?: "Sign-in failed"
                    _isLoading.value = false
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val firebaseRepo = RepositoryProvider.provideFirebaseAuthRepository()
            firebaseRepo?.signOut()
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

    fun clearError() {
        _authError.value = null
    }
}

