package com.hobbeast.app.ui.settings
import androidx.compose.animation.AnimatedVisibility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val REMINDER_OPTIONS = listOf(
    1  to "1 ĂłrĂˇval elĹ‘tte",
    2  to "2 ĂłrĂˇval elĹ‘tte",
    6  to "6 ĂłrĂˇval elĹ‘tte",
    12 to "12 ĂłrĂˇval elĹ‘tte",
    24 to "1 nappal elĹ‘tte",
    48 to "2 nappal elĹ‘tte",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val darkTheme          by viewModel.isDarkTheme.collectAsState()
    val notifications      by viewModel.notificationsEnabled.collectAsState()
    val reminderHours      by viewModel.reminderHours.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeĂˇllĂ­tĂˇsok", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            // â”€â”€â”€ MegjelenĂ©s â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SettingsSection("MegjelenĂ©s") {
                SettingsToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "SĂ¶tĂ©t mĂłd",
                    subtitle = "SĂ¶tĂ©t hĂˇttĂ©r Ă©s szĂ¶vegszĂ­n",
                    checked = darkTheme,
                    onCheckedChange = viewModel::setDarkTheme,
                )
            }

            // â”€â”€â”€ Ă‰rtesĂ­tĂ©sek â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SettingsSection("Ă‰rtesĂ­tĂ©sek") {
                SettingsToggleRow(
                    icon = Icons.Outlined.Notifications,
                    title = "EsemĂ©ny Ă©rtesĂ­tĂ©sek",
                    subtitle = "Push Ă©rtesĂ­tĂ©sek bekapcsolt esemĂ©nyekrĹ‘l",
                    checked = notifications,
                    onCheckedChange = viewModel::setNotifications,
                )

                AnimatedVisibility(notifications) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(thickness = 0.5.dp)
                        Text(
                            "EmlĂ©keztetĹ‘ idĹ‘pontja",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        REMINDER_OPTIONS.forEach { (hours, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                RadioButton(
                                    selected = reminderHours == hours,
                                    onClick = { viewModel.setReminderHours(hours) },
                                )
                            }
                        }
                    }
                }
            }

            // â”€â”€â”€ Az alkalmazĂˇsrĂłl â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SettingsSection("Az alkalmazĂˇsrĂłl") {
                SettingsInfoRow(Icons.Outlined.Info,     "VerziĂł",         "1.0.0 (build 1)")
                SettingsInfoRow(Icons.Outlined.Policy,   "AdatvĂ©delem",    "MegnyitĂˇs")
                SettingsInfoRow(Icons.Outlined.Article,  "FelhasznĂˇlĂˇsi feltĂ©telek", "MegnyitĂˇs")
                SettingsInfoRow(Icons.Outlined.Email,    "Kapcsolat",      "hello@hobbeast.app")
            }
        }
    }
}

// â”€â”€â”€ Reusable setting row components â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(thickness = 0.5.dp)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

