package com.hobbeast.app.ui.createevent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val CATEGORIES = listOf(
    "Zene", "Sport", "Gasztronómia", "Kultúra", "Outdoor",
    "Tech", "Jótékonyság", "Party", "Wellness", "Hobbi",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel(),
) = EventFormScreen(isEdit = false, onBack = onBack, onSaved = onCreated, viewModel = viewModel)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    eventId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel(),
) = EventFormScreen(isEdit = true, onBack = onBack, onSaved = { onSaved() }, viewModel = viewModel)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFormScreen(
    isEdit: Boolean,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: CreateEventViewModel,
) {
    val form by viewModel.form.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val suggestions by viewModel.locationSuggestions.collectAsState()
    var validationErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is CreateEventUiState.Success -> onSaved(s.eventId)
            is CreateEventUiState.ValidationError -> validationErrors = s.errors
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Esemény szerkesztése" else "Új esemény", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Mégse") } },
                actions = {
                    Button(
                        onClick = viewModel::saveEvent,
                        enabled = uiState !is CreateEventUiState.Loading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        if (uiState is CreateEventUiState.Loading)
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Mentés")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ─── Alapadatok ───────────────────────────────────────────────────
            FormSection(title = "Alapadatok") {
                FormField(
                    value = form.title,
                    onValueChange = viewModel::updateTitle,
                    label = "Esemény neve *",
                    error = validationErrors["title"],
                )
                FormField(
                    value = form.description,
                    onValueChange = viewModel::updateDescription,
                    label = "Leírás",
                    minLines = 3, maxLines = 6,
                )
                CategoryDropdown(selected = form.category, onSelect = viewModel::updateCategory,
                    error = validationErrors["category"])
                FormField(
                    value = form.tags,
                    onValueChange = viewModel::updateTags,
                    label = "Címkék (vesszővel elválasztva)",
                )
            }

            // ─── Helyszín ─────────────────────────────────────────────────────
            FormSection(title = "Helyszín") {
                Column {
                    FormField(
                        value = form.location,
                        onValueChange = {
                            viewModel.updateLocation(it)
                            viewModel.searchLocations(it)
                        },
                        label = "Helyszín neve *",
                        error = validationErrors["location"],
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    )
                    if (suggestions.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)) {
                            suggestions.take(5).forEach { suggestion ->
                                ListItem(
                                    headlineContent = { Text(suggestion.label, style = MaterialTheme.typography.bodySmall) },
                                    leadingContent = { Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.clickable { viewModel.selectLocation(suggestion) }
                                )
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
                FormField(
                    value = form.address,
                    onValueChange = viewModel::updateAddress,
                    label = "Cím",
                )
            }

            // ─── Időpont ──────────────────────────────────────────────────────
            FormSection(title = "Időpont") {
                FormField(
                    value = form.startTime,
                    onValueChange = viewModel::updateStartTime,
                    label = "Kezdés (YYYY-MM-DDTHH:MM) *",
                    error = validationErrors["startTime"],
                    leadingIcon = { Icon(Icons.Default.Schedule, null) },
                )
                FormField(
                    value = form.endTime,
                    onValueChange = viewModel::updateEndTime,
                    label = "Befejezés (opcionális)",
                    leadingIcon = { Icon(Icons.Default.Schedule, null) },
                )
            }

            // ─── Résztvevők ───────────────────────────────────────────────────
            FormSection(title = "Résztvevők") {
                FormField(
                    value = form.maxCapacity,
                    onValueChange = viewModel::updateCapacity,
                    label = "Max. létszám (opcionális)",
                    leadingIcon = { Icon(Icons.Default.Group, null) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Privát esemény", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = form.isPrivate, onCheckedChange = viewModel::updateIsPrivate)
                }
            }

            // ─── Jegy / Ár ────────────────────────────────────────────────────
            FormSection(title = "Belépő") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Ingyenes esemény", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = form.isFree, onCheckedChange = viewModel::updateIsFree)
                }
                if (!form.isFree) {
                    FormField(
                        value = form.price,
                        onValueChange = viewModel::updatePrice,
                        label = "Belépő ára (Ft) *",
                        error = validationErrors["price"],
                        leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null) },
                    )
                }
            }

            if (uiState is CreateEventUiState.Error) {
                Text((uiState as CreateEventUiState.Error).message,
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(thickness = 0.5.dp)
        content()
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = leadingIcon,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(selected: String, onSelect: (String) -> Unit, error: String?) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.ifBlank { "Válassz kategóriát" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategória *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            shape = RoundedCornerShape(12.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CATEGORIES.forEach { cat ->
                DropdownMenuItem(text = { Text(cat) }, onClick = { onSelect(cat); expanded = false })
            }
        }
    }
}

