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
import com.dx.tictactoemp.di.RepositoryProvider
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
    val isFirebaseMode = RepositoryProvider.isFirebaseMode()

    LaunchedEffect(roomId, currentUser) {
        val userId = currentUser?.id ?: ""
        gameViewModel.observeGame(roomId, userId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
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
                        if (isFirebaseMode && uiState.mySymbol != null) {
                            Text("You: ${uiState.mySymbol}")
                        }
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
            val status = when {
                uiState.winnerSymbol != null -> {
                    if (isFirebaseMode && uiState.mySymbol == uiState.winnerSymbol) {
                        "You Win!"
                    } else {
                        "Winner: ${uiState.winnerSymbol}"
                    }
                }
                uiState.isDraw -> "Result: Draw"
                uiState.isFinished -> "Finished"
                isFirebaseMode && uiState.isMyTurn -> "Your turn (${uiState.nextSymbol})"
                isFirebaseMode -> "Turn: ${uiState.nextSymbol}"
                else -> "Turn: ${uiState.nextSymbol}"
            }
            Text(
                text = status,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    uiState.winnerSymbol != null -> MaterialTheme.colorScheme.primary
                    uiState.isDraw -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            GameBoard(
                board = uiState.board,
                onCellClick = { index ->
                    val canPlay = if (isFirebaseMode) {
                        !uiState.isFinished && uiState.isMyTurn && uiState.board[index] == null
                    } else {
                        !uiState.isFinished && uiState.board[index] == null
                    }
                    if (canPlay) gameViewModel.submitMove(index)
                },
                enabled = if (isFirebaseMode) !uiState.isFinished && uiState.isMyTurn else !uiState.isFinished
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (uiState.isFinished) {
                Button(
                    onClick = { gameViewModel.rematch() },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Rematch", fontSize = 18.sp) }
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
