package com.hobbeast.app.ui.discovery

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hobbeast.app.data.model.*
import com.hobbeast.app.ui.theme.HobbeastColors

private val CATEGORIES = listOf(
    "Zene", "Sport", "Gasztronómia", "Kultúra", "Outdoor",
    "Tech", "Jótékonyság", "Party", "Wellness", "Hobbi",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    onEventClick: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onProfile: () -> Unit,
    onOrganizerMode: () -> Unit,
    viewModel: DiscoveryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val trending by viewModel.trendingEvents.collectAsState()
    val featured by viewModel.featuredEvents.collectAsState()

    Scaffold(
        topBar = {
            DiscoveryTopBar(
                onProfile = onProfile,
                onOrganizerMode = onOrganizerMode,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Esemény létrehozása")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Filter mode switcher
            FilterModeBar(
                currentMode = filter.mode,
                onModeChange = viewModel::setFilterMode,
            )

            // Search bar (visible when SEARCH mode)
            AnimatedVisibility(
                visible = filter.mode == FilterMode.SEARCH,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SearchBar(
                    query = filter.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Category chips (visible when CATEGORIES mode)
            AnimatedVisibility(visible = filter.mode == FilterMode.CATEGORIES) {
                CategoryChipRow(
                    selected = filter.categories,
                    onToggle = viewModel::toggleCategory,
                )
            }

            // Content
            when (val state = uiState) {
                is DiscoveryUiState.Loading -> FullScreenLoading()
                is DiscoveryUiState.Error -> ErrorState(message = state.message, onRetry = viewModel::refresh)
                is DiscoveryUiState.Success -> EventList(
                    events = state.events,
                    trending = trending,
                    featured = featured,
                    filterMode = filter.mode,
                    onEventClick = onEventClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoveryTopBar(
    onProfile: () -> Unit,
    onOrganizerMode: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                "Hobbeast",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        actions = {
            IconButton(onClick = onOrganizerMode) {
                Icon(Icons.Default.Dashboard, "Organizer mód")
            }
            IconButton(onClick = onProfile) {
                Icon(Icons.Default.AccountCircle, "Profil")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun FilterModeBar(
    currentMode: FilterMode,
    onModeChange: (FilterMode) -> Unit,
) {
    val modes = listOf(
        FilterMode.ALL to "Összes",
        FilterMode.FOR_ME to "Nekem",
        FilterMode.SEARCH to "Keresés",
        FilterMode.CATEGORIES to "Kategóriák",
    )

    ScrollableTabRow(
        selectedTabIndex = modes.indexOfFirst { it.first == currentMode }.coerceAtLeast(0),
        edgePadding = 16.dp,
        divider = {},
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        modes.forEach { (mode, label) ->
            Tab(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                text = {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (currentMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Esemény keresése...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Törlés")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun CategoryChipRow(
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CATEGORIES) { cat ->
            FilterChip(
                selected = cat in selected,
                onClick = { onToggle(cat) },
                label = { Text(cat) },
            )
        }
    }
}

@Composable
private fun EventList(
    events: List<Event>,
    trending: List<Event>,
    featured: List<Event>,
    filterMode: FilterMode,
    onEventClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Trending rail – only in ALL / FOR_ME mode
        if (filterMode in listOf(FilterMode.ALL, FilterMode.FOR_ME) && trending.isNotEmpty()) {
            item {
                SectionHeader(title = "🔥 Trending", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                HorizontalEventRail(events = trending.take(6), onEventClick = onEventClick)
            }
        }

        // Featured rail
        if (filterMode == FilterMode.ALL && featured.isNotEmpty()) {
            item {
                SectionHeader(title = "⭐ Kiemelt", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                HorizontalEventRail(events = featured.take(6), onEventClick = onEventClick)
            }
        }

        // Divider
        if (filterMode in listOf(FilterMode.ALL, FilterMode.FOR_ME)) {
            item {
                SectionHeader(
                    title = when (filterMode) {
                        FilterMode.FOR_ME -> "Neked ajánlott"
                        else -> "Összes esemény"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        if (events.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.EventBusy, contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("Nincs találat", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event.id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun HorizontalEventRail(
    events: List<Event>,
    onEventClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(events, key = { it.id }) { event ->
            CompactEventCard(event = event, onClick = { onEventClick(event.id) })
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            // Hero image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(HobbeastColors.Coral500.copy(alpha = 0.7f), HobbeastColors.Amber500.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White)
                    }
                }
                // Source badge
                if (event.source != EventSource.HOBBEAST) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            event.source.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                // Early access badge
                if (event.isEarlyAccess) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        color = HobbeastColors.Amber500,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text("Early Access", style = MaterialTheme.typography.labelSmall,
                            color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            // Content
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (!event.isFree && event.price != null) {
                        Text(
                            text = "%.0f Ft".format(event.price),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else if (event.isFree) {
                        Text("Ingyenes", style = MaterialTheme.typography.labelSmall, color = HobbeastColors.Success)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatEventTime(event.startTime), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(event.location, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                    }
                }
                // Capacity indicator
                if (event.maxCapacity != null) {
                    Spacer(Modifier.height(8.dp))
                    CapacityBar(current = event.attendeeCount, max = event.maxCapacity)
                }
                // Participation state chip
                if (event.participationState != ParticipationState.NONE) {
                    Spacer(Modifier.height(6.dp))
                    ParticipationChip(state = event.participationState)
                }
            }
        }
    }
}

@Composable
private fun CompactEventCard(
    event: Event,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                if (event.imageUrl != null) {
                    AsyncImage(model = event.imageUrl, contentDescription = event.title,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(HobbeastColors.Coral500, HobbeastColors.Amber500))))
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(event.title, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(formatEventTime(event.startTime), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CapacityBar(current: Int, max: Int) {
    val fraction = (current.toFloat() / max).coerceIn(0f, 1f)
    val color = when {
        fraction >= 0.9f -> HobbeastColors.Error
        fraction >= 0.7f -> HobbeastColors.Warning
        else -> HobbeastColors.Success
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$current / $max résztvevő", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (fraction >= 0.9f) Text("Majdnem teli", style = MaterialTheme.typography.labelSmall, color = HobbeastColors.Error)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun ParticipationChip(state: ParticipationState) {
    val (label, color) = when (state) {
        ParticipationState.GOING -> "Megyek ✓" to HobbeastColors.Success
        ParticipationState.INTERESTED -> "Érdekel" to HobbeastColors.Info
        ParticipationState.WAITLISTED -> "Várólistán" to HobbeastColors.Warning
        ParticipationState.CHECKED_IN -> "Bejelentkezve ✓" to HobbeastColors.Success
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
private fun FullScreenLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null,
                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Újra") }
        }
    }
}

private fun formatEventTime(isoTime: String): String {
    return try {
        val parts = isoTime.split("T")
        if (parts.size == 2) "${parts[0]}  ${parts[1].take(5)}" else isoTime
    } catch (e: Exception) { isoTime }
}
