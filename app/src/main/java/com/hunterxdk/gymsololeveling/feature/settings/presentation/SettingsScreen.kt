package com.hunterxdk.gymsololeveling.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunterxdk.gymsololeveling.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onEquipment: () -> Unit,
    onInjuries: () -> Unit,
    onPriorityMuscles: () -> Unit,
    onTrainingSchedule: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val preferredUnit by viewModel.preferredUnit.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val challengeDifficulty by viewModel.challengeDifficulty.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteAccountState.collectAsStateWithLifecycle()
    val exerciseLocale by viewModel.exerciseLocale.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    val currentLocaleDisplay = if (exerciseLocale.isBlank()) "System Default"
        else LANGUAGE_DISPLAY_NAMES[exerciseLocale] ?: exerciseLocale

    LaunchedEffect(deleteState) {
        if (deleteState is SettingsViewModel.DeleteState.Success) onSignedOut()
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onConfirm = { h, m -> viewModel.setReminderTime(h, m); showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("All your workout data and progress will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = viewModel::deleteAccount,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    if (deleteState is SettingsViewModel.DeleteState.Loading) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Delete Forever")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsToggleRow("Dark Mode", isDarkTheme, onToggle = viewModel::setDarkTheme)
            }
            item {
                SettingsSegmentRow(
                    title = "Weight Unit",
                    options = listOf("kg", "lbs"),
                    selected = preferredUnit,
                    onSelected = viewModel::setUnit,
                )
            }

            item { SettingsSectionHeader("Notifications") }
            item {
                SettingsToggleRow(
                    "Workout Reminders",
                    reminderEnabled,
                    onToggle = viewModel::setReminderEnabled,
                )
            }
            if (reminderEnabled) {
                item {
                    SettingsClickRow(
                        title = "Reminder Time",
                        subtitle = "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}",
                        onClick = { showTimePicker = true },
                    )
                }
            }

            item { SettingsSectionHeader("Training") }
            item {
                SettingsSegmentRow(
                    title = "Challenge Difficulty",
                    options = listOf("EASY", "MEDIUM", "HARD"),
                    selected = challengeDifficulty,
                    onSelected = viewModel::setChallengeDifficulty,
                )
            }
            item { SettingsClickRow("Equipment", "Update available equipment", onClick = onEquipment) }
            item { SettingsClickRow("Injuries", "Set muscle restrictions", onClick = onInjuries) }
            item { SettingsClickRow("Priority Muscles", "Focus your training", onClick = onPriorityMuscles) }
            item { SettingsClickRow("Training Schedule", "Set preferred workout days", onClick = onTrainingSchedule) }
            item {
                SettingsClickRow(
                    title = "Exercise Language",
                    subtitle = currentLocaleDisplay,
                    onClick = { showLanguagePicker = true },
                )
            }

            item { SettingsSectionHeader("Account") }
            item {
                ListItem(
                    headlineContent = {
                        Text("Delete Account", color = MaterialTheme.colorScheme.error)
                    },
                    supportingContent = {
                        Text(
                            "Permanently delete all data",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    modifier = Modifier.clickable { showDeleteDialog = true },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }

            item { SettingsSectionHeader("About") }
            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    trailingContent = {
                        Text(
                            "1.0.0",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Terms of Service") },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    },
                    modifier = Modifier.clickable { /* Open terms */ },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        }
    }

    // Language picker bottom sheet
    if (showLanguagePicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLanguagePicker = false },
            sheetState = sheetState,
        ) {
            Text(
                "Exercise Language",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text("System Default") },
                        leadingContent = {
                            if (exerciseLocale.isBlank()) Icon(Icons.Default.Check, null, tint = GoldAccent)
                        },
                        modifier = Modifier.clickable {
                            viewModel.setLocale("")
                            showLanguagePicker = false
                        },
                    )
                }
                items(LANGUAGE_DISPLAY_NAMES.entries.toList()) { (tag, name) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text(tag, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = {
                            if (exerciseLocale == tag) Icon(Icons.Default.Check, null, tint = GoldAccent)
                        },
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setLocale(tag)
                            showLanguagePicker = false
                        },
                    )
                }
            }
        }
    }
}

private val LANGUAGE_DISPLAY_NAMES = mapOf(
    "en"      to "English",
    "de"      to "German (Deutsch)",
    "de-DE"   to "German — Germany",
    "es"      to "Spanish (Español)",
    "es-ES"   to "Spanish — Spain",
    "es-MX"   to "Spanish — Mexico",
    "fr"      to "French (Français)",
    "fr-CA"   to "French — Canada",
    "fr-FR"   to "French — France",
    "pt"      to "Portuguese (Português)",
    "pt-BR"   to "Portuguese — Brazil",
    "pt-PT"   to "Portuguese — Portugal",
    "it"      to "Italian (Italiano)",
    "nl"      to "Dutch (Nederlands)",
    "nl-NL"   to "Dutch — Netherlands",
    "pl"      to "Polish (Polski)",
    "ru"      to "Russian (Русский)",
    "ja"      to "Japanese (日本語)",
    "ko"      to "Korean (한국어)",
    "zh"      to "Chinese (中文)",
    "zh-Hans" to "Chinese Simplified",
    "zh-Hant" to "Chinese Traditional",
    "ar"      to "Arabic (العربية)",
    "ar-EG"   to "Arabic — Egypt",
    "ar-SA"   to "Arabic — Saudi Arabia",
    "hi"      to "Hindi (हिन्दी)",
    "tr"      to "Turkish (Türkçe)",
    "id"      to "Indonesian",
    "vi"      to "Vietnamese (Tiếng Việt)",
    "th"      to "Thai (ไทย)",
    "uk"      to "Ukrainian (Українська)",
    "sv"      to "Swedish (Svenska)",
    "no"      to "Norwegian (Norsk)",
    "da"      to "Danish (Dansk)",
    "fi"      to "Finnish (Suomi)",
    "cs"      to "Czech (Čeština)",
    "hu"      to "Hungarian (Magyar)",
    "ro"      to "Romanian (Română)",
    "sk"      to "Slovak (Slovenčina)",
    "hr"      to "Croatian (Hrvatski)",
    "he"      to "Hebrew (עברית)",
    "ca"      to "Catalan",
    "ms"      to "Malay",
    "el"      to "Greek (Ελληνικά)",
    "en-AU"   to "English — Australia",
    "en-CA"   to "English — Canada",
    "en-GB"   to "English — UK",
    "en-US"   to "English — US",
)

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
fun SettingsToggleRow(title: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = value, onCheckedChange = onToggle) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun SettingsClickRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun SettingsSegmentRow(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val isSelected = selected == option
                    Surface(
                        onClick = { onSelected(option) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            option.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(initialHour, initialMinute)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder Time") },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            Button(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Set")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
