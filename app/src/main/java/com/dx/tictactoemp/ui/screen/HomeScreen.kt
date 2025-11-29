package com.dx.tictactoemp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dx.tictactoemp.di.RepositoryProvider
import com.dx.tictactoemp.viewmodel.AuthViewModel

@Composable
fun HomeScreen(
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val authError by authViewModel.authError.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val isFirebaseMode = RepositoryProvider.isFirebaseMode()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }

    // Update displayName when user changes
    LaunchedEffect(currentUser?.displayName) {
        displayName = currentUser?.displayName ?: ""
    }

    // Show auth errors
    LaunchedEffect(authError) {
        authError?.let { error ->
            snackbarHostState.showSnackbar(error)
            authViewModel.clearError()
        }
    }

    Scaffold(
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
                text = "Tic Tac Toe",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isFirebaseMode) {
                // Firebase mode: require sign-in
                if (currentUser == null) {
                    Text(
                        text = "Sign in to play online",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Button(
                        onClick = { authViewModel.signInWithGoogle() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Sign in with Google", fontSize = 18.sp)
                        }
                    }
                } else {
                    // Signed in
                    Text(
                        text = "Signed in as ${currentUser?.displayName ?: "User"}",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = onCreateRoom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Create Room", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onJoinRoom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Join Room", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedButton(
                        onClick = { authViewModel.signOut() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                }
            } else {
                // Local mode: no sign-in required
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        authViewModel.updateDisplayName(it)
                    },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onCreateRoom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Create Room", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onJoinRoom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Join Room", fontSize = 18.sp)
                }
            }
        }
    }
}

