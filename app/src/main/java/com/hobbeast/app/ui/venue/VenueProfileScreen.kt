package com.hobbeast.app.ui.venue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hobbeast.app.data.model.Venue
import com.hobbeast.app.ui.discovery.EventCard
import com.hobbeast.app.ui.theme.HobbeastColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueProfileScreen(
    venueId: String,
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: VenueViewModel = hiltViewModel(),
) {
    val venue by viewModel.venue.collectAsState()
    val events by viewModel.events.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(venue?.name ?: "Helyszín") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { padding ->
        if (venue == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val v = venue!!
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.padding(padding),
        ) {
            // Hero
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    if (v.imageUrl != null) {
                        AsyncImage(model = v.imageUrl, contentDescription = v.name,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Place, null, modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (v.isPartner) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                            color = HobbeastColors.Amber500,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, null, modifier = Modifier.size(12.dp), tint = androidx.compose.ui.graphics.Color.White)
                                Text("Partner helyszín", style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
            }

            // Info
            item {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(v.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    if (v.rating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { i ->
                                Icon(
                                    if (i < (v.rating!! + 0.5).toInt()) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = HobbeastColors.Amber500,
                                )
                            }
                            Text("%.1f".format(v.rating), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    VenueInfoRow(Icons.Outlined.LocationOn, v.address)
                    v.phone?.let { VenueInfoRow(Icons.Outlined.Phone, it) }
                    v.website?.let { VenueInfoRow(Icons.Outlined.Language, it) }
                    v.openingHours?.let { VenueInfoRow(Icons.Outlined.Schedule, it) }

                    // Partner capabilities
                    if (v.partnerCapabilities.isNotEmpty()) {
                        Text("Partner lehetőségek", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            v.partnerCapabilities.forEach { cap ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(cap, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp)) },
                                )
                            }
                        }
                    }

                    if (v.description != null) {
                        Text("Leírás", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(v.description, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
            }

            // Events at this venue
            if (events.isNotEmpty()) {
                item {
                    Text("Események itt",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
                items(events, key = { it.id }) { event ->
                    EventCard(event = event, onClick = { onEventClick(event.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun VenueInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
