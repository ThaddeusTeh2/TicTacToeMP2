package com.dx.tictactoemp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dx.tictactoemp.viewmodel.AuthViewModel
import com.dx.tictactoemp.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    roomId: String,
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val uiState by gameViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe game state
    LaunchedEffect(roomId, currentUser) {
        currentUser?.let { user ->
            gameViewModel.observeGame(roomId, user.id)
        }
    }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            gameViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Code: ${uiState.room?.code ?: "----"}")
                        Text("You: ${uiState.mySymbol ?: "-"}")
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status text
            Text(
                text = when {
                    uiState.winnerSymbol != null -> "Winner: ${uiState.winnerSymbol}"
                    uiState.isDraw -> "Result: Draw"
                    uiState.isMyTurn -> "Turn: ${uiState.currentTurnSymbol} (You)"
                    else -> "Turn: ${uiState.currentTurnSymbol}"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    uiState.winnerSymbol != null -> MaterialTheme.colorScheme.primary
                    uiState.isDraw -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Game board
            GameBoard(
                board = uiState.board,
                onCellClick = { index ->
                    if (!uiState.isFinished && uiState.isMyTurn) {
                        gameViewModel.submitMove(index)
                    }
                },
                enabled = !uiState.isFinished && uiState.isMyTurn
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Rematch button (only visible when game finished)
            if (uiState.isFinished) {
                Button(
                    onClick = { gameViewModel.rematch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Rematch", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun GameBoard(
    board: List<Char?>,
    onCellClick: (Int) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    Cell(
                        value = board[index],
                        onClick = { onCellClick(index) },
                        enabled = enabled && board[index] == null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun Cell(
    value: Char?,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .border(2.dp, MaterialTheme.colorScheme.outline)
            .background(
                if (enabled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value?.toString() ?: "",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = when (value) {
                'X' -> MaterialTheme.colorScheme.primary
                'O' -> MaterialTheme.colorScheme.secondary
                else -> Color.Transparent
            }
        )
    }
}

