package com.dx.tictactoemp.data.repository

import com.dx.tictactoemp.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentUserFlow(): Flow<User?>
    suspend fun setCurrentUser(user: User)
    fun getCurrentUser(): User?
}

