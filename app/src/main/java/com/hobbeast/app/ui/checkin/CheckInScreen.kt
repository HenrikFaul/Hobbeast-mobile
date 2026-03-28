package com.hobbeast.app.ui.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hobbeast.app.ui.organizer.CheckInUiState
import com.hobbeast.app.ui.organizer.CheckInViewModel
import com.hobbeast.app.ui.theme.HobbeastColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentCheckIns by viewModel.recentCheckIns.collectAsState()
    var inviteCode by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    if (showScanner) {
        QrScannerView(
            onCodeScanned = { code ->
                showScanner = false
                inviteCode = code
                viewModel.checkIn(code)
            },
            onClose = { showScanner = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-in", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    Text(
                        "${recentCheckIns.size} bejelentkezve",
                        style = MaterialTheme.typography.labelMedium,
                        color = HobbeastColors.Success,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ─── QR scanner button ────────────────────────────────────────────
            Button(
                onClick = { showScanner = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("QR kód szkenner", style = MaterialTheme.typography.titleMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("  vagy  ", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // ─── Manual code entry ────────────────────────────────────────────
            Text("Meghívókód manuális beírása", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase().take(12) },
                label = { Text("Meghívókód (pl. HOBBEAST42)") },
                leadingIcon = { Icon(Icons.Default.Key, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Button(
                onClick = { viewModel.checkIn(inviteCode) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = inviteCode.length >= 4 && uiState !is CheckInUiState.Loading,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState is CheckInUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Ellenőrzés...")
                } else {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Check-in")
                }
            }

            // ─── Result banner ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState is CheckInUiState.Success || uiState is CheckInUiState.Error,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                when (val s = uiState) {
                    is CheckInUiState.Success -> ResultBanner(
                        text = "✓ ${s.attendeeName} – Bejelentkezve!",
                        containerColor = HobbeastColors.Success.copy(alpha = 0.12f),
                        textColor = HobbeastColors.Success,
                    )
                    is CheckInUiState.Error -> ResultBanner(
                        text = "✗ ${s.message}",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        textColor = MaterialTheme.colorScheme.error,
                    )
                    else -> {}
                }
            }

            // ─── Recent check-ins list ────────────────────────────────────────
            if (recentCheckIns.isNotEmpty()) {
                HorizontalDivider()
                Text("Legutóbbi bejelentkezések (${recentCheckIns.size})",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(recentCheckIns) { name ->
                        ListItem(
                            headlineContent = { Text(name) },
                            leadingContent = {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = HobbeastColors.Success, modifier = Modifier.size(20.dp))
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = HobbeastColors.Success.copy(alpha = 0.05f),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultBanner(text: String, containerColor: Color, textColor: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
