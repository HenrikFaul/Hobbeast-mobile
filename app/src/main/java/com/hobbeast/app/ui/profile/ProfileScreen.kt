package com.hobbeast.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hobbeast.app.data.model.ProfileVisibility
import com.hobbeast.app.ui.theme.HobbeastColors

private val ALL_INTERESTS = listOf(
    "Zene", "Sport", "Futás", "Kerékpározás", "Kirándulás",
    "Gasztronómia", "Kultúra", "Tech", "Könyvek", "Film", "Jóga",
    "Tánc", "Fotózás", "Festészet", "Kézimunka", "Gaming",
    "Természet", "Utazás", "Jótékonyság", "Party",
)

private val DISTANCES = listOf(5, 10, 25, 50, 100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    Button(
                        onClick = viewModel::save,
                        enabled = saveState !is ProfileSaveState.Saving,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        if (saveState is ProfileSaveState.Saving)
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Mentés")
                    }
                },
            )
        },
    ) { padding ->
        profile?.let { p ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Avatar
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    if (p.avatarUrl != null) {
                        AsyncImage(model = p.avatarUrl, contentDescription = "Avatar",
                            modifier = Modifier.size(96.dp).clip(CircleShape))
                    } else {
                        Surface(modifier = Modifier.size(96.dp), shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.padding(4.dp),
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                // Basic info
                ProfileSection("Alapadatok") {
                    ProfileTextField("Megjelenített név", p.displayName, viewModel::updateDisplayName)
                    ProfileTextField("Bemutatkozás", p.bio ?: "", viewModel::updateBio, maxLines = 3)
                    ProfileTextField("Helyszín", p.location ?: "", viewModel::updateLocation,
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) })
                }

                // Privacy
                ProfileSection("Adatvédelem") {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Helyszín megosztása", style = MaterialTheme.typography.bodyMedium)
                            Text("Távolságalapú ajánlásokhoz szükséges",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = p.locationSharing, onCheckedChange = { viewModel.toggleLocationSharing() })
                    }
                    Text("Profil láthatósága", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileVisibility.values().forEach { v ->
                            FilterChip(selected = p.profileVisibility == v, onClick = { viewModel.updateVisibility(v) },
                                label = { Text(v.label()) })
                        }
                    }
                }

                // Distance filter
                ProfileSection("Eseménytávolság") {
                    Text("Legfeljebb ${p.distanceKm} km-es eseményeket mutat",
                        style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = p.distanceKm.toFloat(),
                        onValueChange = { viewModel.setDistanceKm(it.toInt()) },
                        valueRange = 1f..100f,
                        steps = 9,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1 km", style = MaterialTheme.typography.labelSmall)
                        Text("100 km", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Interests
                ProfileSection("Érdeklődési körök") {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ALL_INTERESTS.forEach { interest ->
                            FilterChip(selected = interest in p.interests,
                                onClick = { viewModel.toggleInterest(interest) }, label = { Text(interest) })
                        }
                    }
                }

                // Sign out
                OutlinedButton(
                    onClick = { viewModel.signOut(onSignOut) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kijelentkezés")
                }

                if (saveState is ProfileSaveState.Saved) {
                    Card(colors = CardDefaults.cardColors(containerColor = HobbeastColors.Success.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = HobbeastColors.Success)
                            Spacer(Modifier.width(8.dp))
                            Text("Profil elmentve!", color = HobbeastColors.Success)
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(thickness = 0.5.dp)
        content()
    }
}

@Composable
private fun ProfileTextField(
    label: String, value: String, onValueChange: (String) -> Unit,
    maxLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), maxLines = maxLines, singleLine = maxLines == 1,
        shape = RoundedCornerShape(12.dp), leadingIcon = leadingIcon,
    )
}

private fun ProfileVisibility.label() = when (this) {
    ProfileVisibility.PUBLIC  -> "Nyilvános"
    ProfileVisibility.FRIENDS -> "Ismerősök"
    ProfileVisibility.PRIVATE -> "Privát"
}
