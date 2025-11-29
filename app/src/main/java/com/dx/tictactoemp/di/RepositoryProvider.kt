package com.dx.tictactoemp.di

import android.content.Context
import com.dx.tictactoemp.data.repository.*

object RepositoryProvider {
    private var context: Context? = null
    private var useFirebase: Boolean = false

    private val localAuthRepository: AuthRepository by lazy { LocalAuthRepository() }
    private val localGameRepository: GameRepository by lazy { LocalGameRepository() }

    private val firebaseAuthRepository: FirebaseAuthRepository? by lazy {
        context?.let { FirebaseAuthRepository(it) }
    }
    private val firebaseGameRepository: GameRepository by lazy { FirebaseGameRepository() }

    fun initialize(appContext: Context, firebase: Boolean = false) {
        context = appContext
        useFirebase = firebase
    }

    fun setFirebaseMode(enabled: Boolean) {
        useFirebase = enabled
    }

    fun isFirebaseMode(): Boolean = useFirebase

    fun provideAuthRepository(): AuthRepository {
        return if (useFirebase && firebaseAuthRepository != null) {
            firebaseAuthRepository!!
        } else {
            localAuthRepository
        }
    }

    fun provideGameRepository(): GameRepository {
        return if (useFirebase) {
            firebaseGameRepository
        } else {
            localGameRepository
        }
    }

    fun provideFirebaseAuthRepository(): FirebaseAuthRepository? = firebaseAuthRepository
}

