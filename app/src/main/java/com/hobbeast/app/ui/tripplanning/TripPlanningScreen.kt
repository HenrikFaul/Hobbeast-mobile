package com.hobbeast.app.ui.tripplanning

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hobbeast.app.data.model.RouteType
import com.hobbeast.app.data.repository.LocationSuggestion
import com.hobbeast.app.ui.theme.HobbeastColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlanningScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: TripPlanningViewModel = hiltViewModel(),
) {
    val planState by viewModel.planState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val activeField by viewModel.activeField.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ĂštvonaltervezĂ©s", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (planState.routeReady) {
                        IconButton(onClick = viewModel::savePlan) {
                            Icon(if (saveState is SaveState.Saved) Icons.Default.CheckCircle else Icons.Default.Save,
                                "MentĂ©s", tint = if (saveState is SaveState.Saved) HobbeastColors.Success else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (planState.start != null && planState.end != null) {
                Surface(tonalElevation = 4.dp) {
                    Button(
                        onClick = viewModel::planRoute,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                        enabled = !planState.isCalculating,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (planState.isCalculating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Ăštvonal szĂˇmĂ­tĂˇsa...")
                        } else {
                            Icon(Icons.Default.Directions, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (planState.routeReady) "ĂšjratervezĂ©s" else "Ăštvonal tervezĂ©se")
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding),
        ) {
            // â”€â”€â”€ Map placeholder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("TĂ©rkĂ©p nĂ©zet", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("(Mapy.com â€“ MapView)", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // â”€â”€â”€ Route type selector â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                Text("KĂ¶zlekedĂ©si mĂłd", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RouteType.values().forEach { type ->
                        FilterChip(
                            selected = planState.routeType == type,
                            onClick = { viewModel.setRouteType(type) },
                            label = { Text(type.label()) },
                            leadingIcon = { Icon(type.icon(), null, modifier = Modifier.size(16.dp)) },
                        )
                    }
                }
            }

            // â”€â”€â”€ Start point â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                LocationInputField(
                    label = "IndulĂˇsi pont",
                    value = planState.start?.label ?: "",
                    isActive = activeField == TripField.START,
                    onFocus = { viewModel.setActiveField(TripField.START) },
                    onSearch = viewModel::searchLocation,
                    suggestions = if (activeField == TripField.START) suggestions else emptyList(),
                    onSelect = viewModel::selectSuggestion,
                    icon = Icons.Default.TripOrigin,
                    iconTint = HobbeastColors.Success,
                )
            }

            // â”€â”€â”€ Waypoints â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            itemsIndexed(planState.waypoints) { index, waypoint ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = HobbeastColors.Amber500,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(waypoint.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removeWaypoint(index) }) {
                        Icon(Icons.Default.RemoveCircleOutline, "EltĂˇvolĂ­tĂˇs", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // â”€â”€â”€ Add waypoint â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                AnimatedVisibility(visible = activeField == TripField.WAYPOINT) {
                    LocationInputField(
                        label = "KĂ¶zbĂĽlsĹ‘ pont",
                        value = "",
                        isActive = true,
                        onFocus = {},
                        onSearch = viewModel::searchLocation,
                        suggestions = suggestions,
                        onSelect = viewModel::selectSuggestion,
                        icon = Icons.Default.AddLocation,
                        iconTint = HobbeastColors.Amber500,
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.setActiveField(TripField.WAYPOINT) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.AddCircleOutline, null)
                    Spacer(Modifier.width(8.dp))
                    Text("KĂ¶zbĂĽlsĹ‘ pont hozzĂˇadĂˇsa")
                }
            }

            // â”€â”€â”€ End point â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                LocationInputField(
                    label = "Ă‰rkezĂ©si pont",
                    value = planState.end?.label ?: "",
                    isActive = activeField == TripField.END,
                    onFocus = { viewModel.setActiveField(TripField.END) },
                    onSearch = viewModel::searchLocation,
                    suggestions = if (activeField == TripField.END) suggestions else emptyList(),
                    onSelect = viewModel::selectSuggestion,
                    icon = Icons.Default.Place,
                    iconTint = HobbeastColors.Error,
                )
            }

            // â”€â”€â”€ Route summary â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (planState.routeReady) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Straighten, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("%.1f km".format(planState.calculatedDistance ?: 0.0),
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("TĂˇvolsĂˇg", style = MaterialTheme.typography.labelSmall)
                            }
                            VerticalDivider(modifier = Modifier.height(48.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${planState.calculatedDurationMin ?: 0} perc",
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("MenetidĹ‘", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (saveState is SaveState.Saved) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = HobbeastColors.Success.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = HobbeastColors.Success)
                            Spacer(Modifier.width(8.dp))
                            Text("Ăštvonal elmentve az esemĂ©nyhez!", color = HobbeastColors.Success)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationInputField(
    label: String,
    value: String,
    isActive: Boolean,
    onFocus: () -> Unit,
    onSearch: (String) -> Unit,
    suggestions: List<LocationSuggestion>,
    onSelect: (LocationSuggestion) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
) {
    var query by remember(value) { mutableStateOf(value) }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onSearch(it) },
            label = { Text(label) },
            leadingIcon = { Icon(icon, null, tint = iconTint) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = if (suggestions.isNotEmpty()) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp) else RoundedCornerShape(12.dp),
        )
        if (suggestions.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)) {
                suggestions.take(5).forEach { s ->
                    ListItem(
                        headlineContent = { Text(s.label, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = { Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.clickable { onSelect(s) },
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}


private fun RouteType.label() = when (this) {
    RouteType.CAR     -> "AutĂł"
    RouteType.FOOT    -> "Gyalog"
    RouteType.BIKE    -> "KerĂ©kpĂˇr"
    RouteType.TRANSIT -> "TĂ¶megkĂ¶zlekedĂ©s"
}

private fun RouteType.icon() = when (this) {
    RouteType.CAR     -> Icons.Default.DirectionsCar
    RouteType.FOOT    -> Icons.Default.DirectionsWalk
    RouteType.BIKE    -> Icons.Default.DirectionsBike
    RouteType.TRANSIT -> Icons.Default.DirectionsTransit
}

