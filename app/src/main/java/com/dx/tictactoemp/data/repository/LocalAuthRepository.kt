package com.dx.tictactoemp.data.repository

import com.dx.tictactoemp.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LocalAuthRepository : AuthRepository {
    private val currentUserState = MutableStateFlow<User?>(null)

    override fun currentUserFlow(): Flow<User?> = currentUserState

    override suspend fun setCurrentUser(user: User) {
        currentUserState.value = user
    }

    override fun getCurrentUser(): User? = currentUserState.value
}

