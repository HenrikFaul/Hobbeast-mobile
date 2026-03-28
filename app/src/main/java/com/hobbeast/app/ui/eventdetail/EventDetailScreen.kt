package com.hobbeast.app.ui.eventdetail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hobbeast.app.data.model.*
import com.hobbeast.app.ui.discovery.CapacityBar
import com.hobbeast.app.ui.theme.HobbeastColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTripPlanning: () -> Unit,
    onCheckIn: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val participationLoading by viewModel.participationLoading.collectAsState()

    when (val state = uiState) {
        is EventDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is EventDetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::loadEvent) { Text("Újra") }
            }
        }
        is EventDetailUiState.Success -> EventDetailContent(
            event = state.event,
            participationLoading = participationLoading,
            onBack = onBack,
            onEdit = onEdit,
            onTripPlanning = onTripPlanning,
            onCheckIn = onCheckIn,
            onParticipate = viewModel::setParticipation,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailContent(
    event: Event,
    participationLoading: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTripPlanning: () -> Unit,
    onCheckIn: () -> Unit,
    onParticipate: (ParticipationState) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Vissza",
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(50)))
                    }
                },
                actions = {
                    if (event.organizerId.isNotEmpty()) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Szerkesztés")
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, "Megosztás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            RSVPBottomBar(
                event = event,
                loading = participationLoading,
                onParticipate = onParticipate,
                onCheckIn = onCheckIn,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            // Hero image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            ) {
                if (event.imageUrl != null) {
                    AsyncImage(
                        model = event.imageUrl,
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Event, null, modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Content
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Title + category
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(event.category, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (event.source != EventSource.HOBBEAST) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Text(event.source.name, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Text(event.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                // Info rows
                InfoRow(Icons.Outlined.Schedule, formatDate(event.startTime))
                InfoRow(Icons.Outlined.LocationOn, event.address ?: event.location)
                if (!event.isFree) {
                    InfoRow(Icons.Outlined.ConfirmationNumber, event.ticketTiers.firstOrNull()?.let {
                        "${it.name}: ${it.price.toInt()} ${it.currency}"
                    } ?: "Fizetős esemény")
                } else {
                    InfoRow(Icons.Outlined.CheckCircle, "Ingyenes belépés", color = HobbeastColors.Success)
                }
                if (event.organizerName != null) {
                    InfoRow(Icons.Outlined.Person, "Szervező: ${event.organizerName}")
                }

                // Capacity
                if (event.maxCapacity != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Kapacitás", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            CapacityBar(current = event.attendeeCount, max = event.maxCapacity)
                        }
                    }
                }

                // Description
                if (event.description.isNotBlank()) {
                    Text("Leírás", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(event.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
                }

                // Trip planning button
                OutlinedButton(
                    onClick = onTripPlanning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Map, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Útvonaltervezés")
                }

                // Tags
                if (event.tags.isNotEmpty()) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        event.tags.forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RSVPBottomBar(
    event: Event,
    loading: Boolean,
    onParticipate: (ParticipationState) -> Unit,
    onCheckIn: () -> Unit,
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (event.participationState) {
                ParticipationState.NONE, ParticipationState.INTERESTED -> {
                    OutlinedButton(
                        onClick = { onParticipate(ParticipationState.INTERESTED) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !loading,
                    ) { Text("Érdekel") }
                    Button(
                        onClick = { onParticipate(ParticipationState.GOING) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !loading && (event.maxCapacity == null || event.attendeeCount < event.maxCapacity),
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Megyek!")
                    }
                }
                ParticipationState.GOING -> {
                    OutlinedButton(
                        onClick = { onParticipate(ParticipationState.NONE) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Mégsem megyek") }
                    Button(
                        onClick = onCheckIn,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HobbeastColors.Success),
                    ) { Text("Check-in") }
                }
                ParticipationState.WAITLISTED -> {
                    Button(
                        onClick = { onParticipate(ParticipationState.NONE) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HobbeastColors.Warning),
                    ) { Text("Kilépés a várólistáról") }
                }
                ParticipationState.CHECKED_IN -> {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HobbeastColors.Success),
                        enabled = false,
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bejelentkezve ✓")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color)
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}

private fun formatDate(isoTime: String): String {
    return try {
        val parts = isoTime.split("T")
        if (parts.size == 2) "${parts[0]}  ${parts[1].take(5)}" else isoTime
    } catch (e: Exception) { isoTime }
}
