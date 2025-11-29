package com.dx.tictactoemp.data.repository

import android.content.Context
import com.dx.tictactoemp.data.auth.FirebaseAuthService
import com.dx.tictactoemp.model.User
import kotlinx.coroutines.flow.Flow

class FirebaseAuthRepository(
    context: Context
) : AuthRepository {
    private val authService = FirebaseAuthService(context)

    override fun currentUserFlow(): Flow<User?> = authService.currentUserFlow()

    override suspend fun setCurrentUser(user: User) {
        // For Firebase, this is handled by sign-in
        // User creation happens in FirebaseAuthService.upsertUser
    }

    override fun getCurrentUser(): User? = authService.getCurrentUser()

    suspend fun signInWithGoogle(): Result<User> = authService.signInWithGoogle()

    suspend fun signOut() = authService.signOut()
}

