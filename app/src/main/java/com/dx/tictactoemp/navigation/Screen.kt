package com.dx.tictactoemp.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateRoom : Screen("createRoom")
    object JoinRoom : Screen("joinRoom")
    object Game : Screen("game/{roomId}") {
        fun createRoute(roomId: String) = "game/$roomId"
    }
}

