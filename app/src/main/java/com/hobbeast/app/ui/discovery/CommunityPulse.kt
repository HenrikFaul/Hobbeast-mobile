package com.hobbeast.app.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.model.CommunityPulse
import com.hobbeast.app.data.remote.SupabaseDataSource
import com.hobbeast.app.ui.theme.HobbeastColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class CommunityPulseViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
) : ViewModel() {

    private val _pulses = MutableStateFlow<List<CommunityPulse>>(emptyList())
    val pulses: StateFlow<List<CommunityPulse>> = _pulses.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            runCatching { supabase.getCommunityPulse("") }
                .onSuccess { _pulses.value = it }
        }
    }
}

// ─── Community Pulse Section (embedded in discovery or event detail) ──────────

@Composable
fun CommunityPulseSection(
    pulses: List<CommunityPulse>,
    modifier: Modifier = Modifier,
) {
    if (pulses.isEmpty()) return

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp), tint = HobbeastColors.Coral500)
                Text(
                    "Aktív közösségi jelenetek",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            // Privacy badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Aggregált adatok", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(pulses) { pulse ->
                CommunityPulseCard(pulse = pulse)
            }
        }
    }
}

@Composable
fun CommunityPulseCard(pulse: CommunityPulse) {
    val intensity = (pulse.activityScore * 3).toInt().coerceIn(1, 3)
    val (intensityLabel, intensityColor) = when (intensity) {
        3    -> "Nagyon aktív" to HobbeastColors.Coral500
        2    -> "Aktív" to HobbeastColors.Amber500
        else -> "Növekvő" to HobbeastColors.Success
    }

    Card(
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    pulse.sceneLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                // Activity dot
                Box(
                    modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(intensityColor),
                )
            }
            Surface(
                color = intensityColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    intensityLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = intensityColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (pulse.recurringFormats.isNotEmpty()) {
                Text(
                    pulse.recurringFormats.take(2).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pulse.isUnderserved) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, modifier = Modifier.size(12.dp),
                        tint = HobbeastColors.Amber500)
                    Text("Alulszolgált jelenet", style = MaterialTheme.typography.labelSmall,
                        color = HobbeastColors.Amber500)
                }
            }
        }
    }
}

// ─── Organizer demand insight banner (HOB-178) ────────────────────────────────

@Composable
fun OrganizerDemandInsightBanner(
    pulses: List<CommunityPulse>,
    onCreateEvent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val underserved = pulses.filter { it.isUnderserved }
    if (underserved.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = HobbeastColors.Amber500.copy(alpha = 0.1f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TrendingUp, null, tint = HobbeastColors.Amber500, modifier = Modifier.size(20.dp))
                Text("Lehetőség a közösségedben", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = HobbeastColors.Amber500)
            }
            Text(
                "A következő jelenetek aktívak, de hiányoznak a rendszeres események: " +
                        underserved.take(3).joinToString(", ") { it.sceneLabel },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(
                onClick = onCreateEvent,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Esemény létrehozása")
            }
            Text(
                "Az adatok aggregáltak és anonimizáltak. Egyéni felhasználói adatot nem tartalmaz.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
