package com.dx.tictactoemp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dx.tictactoemp.di.RepositoryProvider
import com.dx.tictactoemp.navigation.Screen
import com.dx.tictactoemp.ui.screen.CreateRoomScreen
import com.dx.tictactoemp.ui.screen.GameScreen
import com.dx.tictactoemp.ui.screen.HomeScreen
import com.dx.tictactoemp.ui.screen.JoinRoomScreen
import com.dx.tictactoemp.ui.theme.TicTacToeMPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize RepositoryProvider with Firebase mode enabled
        RepositoryProvider.initialize(applicationContext, firebase = true)

        enableEdgeToEdge()
        setContent {
            TicTacToeMPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TicTacToeApp()
                }
            }
        }
    }
}

@Composable
fun TicTacToeApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onCreateRoom = {
                    navController.navigate(Screen.CreateRoom.route)
                },
                onJoinRoom = {
                    navController.navigate(Screen.JoinRoom.route)
                }
            )
        }

        composable(Screen.CreateRoom.route) {
            CreateRoomScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToGame = { roomId ->
                    navController.navigate(Screen.Game.createRoute(roomId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.JoinRoom.route) {
            JoinRoomScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToGame = { roomId ->
                    navController.navigate(Screen.Game.createRoute(roomId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            GameScreen(
                roomId = roomId,
                onBack = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}