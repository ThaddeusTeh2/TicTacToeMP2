package com.dx.tictactoemp.di

import com.dx.tictactoemp.data.repository.AuthRepository
import com.dx.tictactoemp.data.repository.GameRepository
import com.dx.tictactoemp.data.repository.LocalAuthRepository
import com.dx.tictactoemp.data.repository.LocalGameRepository

object RepositoryProvider {
    private val authRepository: AuthRepository by lazy { LocalAuthRepository() }
    private val gameRepository: GameRepository by lazy { LocalGameRepository() }

    fun provideAuthRepository(): AuthRepository = authRepository
    fun provideGameRepository(): GameRepository = gameRepository
}

