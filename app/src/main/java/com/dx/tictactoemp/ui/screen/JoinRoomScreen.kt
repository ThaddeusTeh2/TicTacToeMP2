package com.dx.tictactoemp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dx.tictactoemp.viewmodel.AuthViewModel
import com.dx.tictactoemp.viewmodel.RoomsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreen(
    onBack: () -> Unit,
    onNavigateToGame: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    roomsViewModel: RoomsViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val uiState by roomsViewModel.uiState.collectAsState()
    var roomCode by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate to game when room is joined
    LaunchedEffect(uiState.joinedRoom) {
        uiState.joinedRoom?.let { room ->
            onNavigateToGame(room.id)
            roomsViewModel.clearJoinedRoom()
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
                title = { Text("Join a Room") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Enter 4-digit room code:",
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = roomCode,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        roomCode = it
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 32.sp,
                    letterSpacing = 8.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    currentUser?.let { user ->
                        roomsViewModel.joinRoom(roomCode, user.id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = roomCode.length == 4 && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Join", fontSize = 18.sp)
                }
            }
        }
    }
}

