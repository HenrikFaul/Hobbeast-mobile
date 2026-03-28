package com.hobbeast.app.ui.retention

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hobbeast.app.data.model.Event
import com.hobbeast.app.service.CalendarService
import com.hobbeast.app.ui.theme.HobbeastColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPlansScreen(
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: MyPlansViewModel = hiltViewModel(),
    calendarService: CalendarService? = null,
) {
    val goingEvents  by viewModel.goingEvents.collectAsState()
    val savedEvents  by viewModel.savedEvents.collectAsState()
    val reminderMap  by viewModel.reminderMap.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terveim", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Megyek (${goingEvents.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Érdekel (${savedEvents.size})") })
            }

            val events = if (selectedTab == 0) goingEvents else savedEvents

            if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Outlined.EventNote, null, modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (selectedTab == 0) "Még nincs esemény, amire mész"
                            else "Még nincs mentett eseményed",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(events, key = { it.id }) { event ->
                        PlanEventCard(
                            event = event,
                            reminderSet = reminderMap[event.id] == true,
                            onEventClick = { onEventClick(event.id) },
                            onToggleReminder = { viewModel.toggleReminder(event) },
                            onAddToCalendar = {
                                calendarService?.let { svc ->
                                    val intent = svc.openAddToCalendarIntent(
                                        title = event.title,
                                        description = event.description,
                                        location = event.address ?: event.location,
                                        startTimeIso = event.startTime,
                                        endTimeIso = event.endTime,
                                    )
                                    context.startActivity(intent)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanEventCard(
    event: Event,
    reminderSet: Boolean,
    onEventClick: () -> Unit,
    onToggleReminder: () -> Unit,
    onAddToCalendar: () -> Unit,
) {
    Card(
        onClick = onEventClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Date badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(54.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val dateParts = event.startTime.take(10).split("-")
                    Text(dateParts.getOrElse(2) { "--" },
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(monthShort(dateParts.getOrElse(1) { "" }),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Title + meta
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(event.location, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(event.startTime.replace("T", "  ").take(16),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Actions
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleReminder, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (reminderSet) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                        "Emlékeztető",
                        modifier = Modifier.size(20.dp),
                        tint = if (reminderSet) HobbeastColors.Coral500 else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onAddToCalendar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, "Naptár", modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun monthShort(month: String): String = when (month) {
    "01" -> "jan"; "02" -> "feb"; "03" -> "márc"; "04" -> "ápr"
    "05" -> "máj"; "06" -> "jún"; "07" -> "júl";  "08" -> "aug"
    "09" -> "szept";"10" -> "okt"; "11" -> "nov"; "12" -> "dec"
    else -> month
}
