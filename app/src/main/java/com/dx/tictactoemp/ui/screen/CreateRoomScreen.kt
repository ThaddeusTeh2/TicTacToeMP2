package com.dx.tictactoemp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dx.tictactoemp.viewmodel.AuthViewModel
import com.dx.tictactoemp.viewmodel.RoomsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    onBack: () -> Unit,
    onNavigateToGame: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    roomsViewModel: RoomsViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val uiState by roomsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Create room on first composition
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            roomsViewModel.createRoom(user.id)
        }
    }

    // Navigate to game when room is created
    LaunchedEffect(uiState.createdRoom) {
        uiState.createdRoom?.let { room ->
            onNavigateToGame(room.id)
            roomsViewModel.clearCreatedRoom()
        }
    }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            roomsViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    uiState.createdRoom?.let { room ->
                        Text("Room Code: ${room.code}")
                    } ?: Text("Creating Room...")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                uiState.createdRoom?.let { room ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Host: You (X)",
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Status: Waiting for player...",
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Share this code:",
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = room.code.chunked(1).joinToString(" "),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = { onNavigateToGame(room.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Enter Game", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

