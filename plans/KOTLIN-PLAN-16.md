# KOTLIN-PLAN-16.md — WeightTrackerScreen, TrainingSchedule, Equipment, Injuries & PriorityMuscles

## Goal
Implement WeightTrackerScreen with Canvas line chart, TrainingScheduleScreen (day picker), EquipmentSettings, InjurySettings, and PriorityMusclesScreen.

## Phase
Features — Phase 5 of 6. Depends on KOTLIN-PLAN-03, KOTLIN-PLAN-15.

---

## Files to Create

### `feature/tracker/presentation/WeightTrackerViewModel.kt`
```kotlin
@HiltViewModel
class WeightTrackerViewModel @Inject constructor(
    private val weightEntryDao: WeightEntryDao,
    private val firestore: FirebaseFirestore,
    private val json: Json,
) : ViewModel() {

    val entries: StateFlow<List<WeightEntry>> = flow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@flow
        weightEntryDao.getAll(uid)
            .map { entities -> entities.map { WeightEntry(it.id, it.userId, it.weightKg, it.recordedAt, it.notes) } }
            .collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(WeightTrackerUiState())
    val uiState: StateFlow<WeightTrackerUiState> = _uiState.asStateFlow()

    fun logWeight(weightKg: Double, notes: String = "") {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val entry = WeightEntryEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = uid,
                weightKg = weightKg,
                recordedAt = System.currentTimeMillis(),
                notes = notes,
            )
            weightEntryDao.insert(entry)

            // Sync to Firestore
            firestore.collection("users").document(uid)
                .collection("weightEntries").document(entry.id)
                .set(mapOf("weightKg" to weightKg, "recordedAt" to entry.recordedAt, "notes" to notes))
        }
    }

    fun deleteEntry(entry: WeightEntry) {
        viewModelScope.launch {
            weightEntryDao.delete(WeightEntryEntity(entry.id, entry.userId, entry.weightKg, entry.recordedAt, entry.notes))
        }
    }
}

data class WeightTrackerUiState(
    val inputWeight: String = "",
    val inputNotes: String = "",
)
```

### `feature/tracker/presentation/WeightTrackerScreen.kt`
```kotlin
@Composable
fun WeightTrackerScreen(
    onBack: () -> Unit,
    viewModel: WeightTrackerViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var inputWeight by remember { mutableStateOf("") }
    var showLogDialog by remember { mutableStateOf(false) }

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("Log Weight") },
            text = {
                OutlinedTextField(
                    value = inputWeight,
                    onValueChange = { inputWeight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    inputWeight.toDoubleOrNull()?.let { w ->
                        viewModel.logWeight(w)
                        inputWeight = ""
                        showLogDialog = false
                    }
                }) { Text("Log") }
            },
            dismissButton = { TextButton(onClick = { showLogDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Tracker") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showLogDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Log weight")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (entries.size >= 2) {
                // Line chart
                WeightLineChart(
                    entries = entries.sortedBy { it.recordedAt },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            // Stats
            if (entries.isNotEmpty()) {
                val sorted = entries.sortedBy { it.recordedAt }
                val latest = sorted.last()
                val earliest = sorted.first()
                val change = latest.weightKg - earliest.weightKg

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${latest.weightKg}kg", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val changeColor = if (change <= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        Text("${if (change >= 0) "+" else ""}${String.format("%.1f", change)}kg", style = MaterialTheme.typography.headlineSmall, color = changeColor)
                        Text("Change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${entries.size}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                        Text("Entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(entries.sortedByDescending { it.recordedAt }, key = { it.id }) { entry ->
                    WeightEntryRow(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                }
            }
        }
    }
}

@Composable
fun WeightLineChart(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    if (entries.size < 2) return

    val minWeight = entries.minOf { it.weightKg }.toFloat()
    val maxWeight = entries.maxOf { it.weightKg }.toFloat()
    val range = (maxWeight - minWeight).coerceAtLeast(1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(surfaceColor).padding(16.dp)) {
        val stepX = size.width / (entries.size - 1)
        val points = entries.mapIndexed { i, entry ->
            val x = i * stepX
            val y = size.height - ((entry.weightKg.toFloat() - minWeight) / range) * size.height
            Offset(x, y)
        }

        // Draw filled area under line
        val path = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(path, primaryColor.copy(alpha = 0.15f))

        // Draw line
        for (i in 0 until points.size - 1) {
            drawLine(primaryColor, points[i], points[i + 1], strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }

        // Draw dots
        points.forEach { point ->
            drawCircle(primaryColor, radius = 4.dp.toPx(), center = point)
        }
    }
}

@Composable
fun WeightEntryRow(entry: WeightEntry, onDelete: () -> Unit) {
    val dateStr = remember(entry.recordedAt) {
        val local = java.time.Instant.ofEpochMilli(entry.recordedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        "${local.dayOfMonth} ${local.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())} ${local.year}"
    }
    ListItem(
        headlineContent = { Text("${entry.weightKg} kg") },
        supportingContent = { Text(dateStr, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}
```

### `feature/settings/presentation/TrainingScheduleScreen.kt`
```kotlin
@Composable
fun TrainingScheduleScreen(
    onBack: () -> Unit,
    viewModel: TrainingScheduleViewModel = hiltViewModel()
) {
    val selectedDays by viewModel.selectedDays.collectAsStateWithLifecycle()
    val days = DayOfWeek.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Schedule") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Select your preferred training days", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days) { day ->
                    val isSelected = day in selectedDays
                    Surface(
                        onClick = { viewModel.toggleDay(day) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.aspectRatio(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Text("${selectedDays.size} days/week selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

            Spacer(Modifier.weight(1f))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Save Schedule") }
        }
    }
}
```

### `feature/settings/presentation/EquipmentSettingsScreen.kt`
```kotlin
@Composable
fun EquipmentSettingsScreen(
    onBack: () -> Unit,
    viewModel: EquipmentSettingsViewModel = hiltViewModel()
) {
    val selected by viewModel.selectedEquipment.collectAsStateWithLifecycle()
    val equipmentOptions = listOf(
        "barbell" to "Barbell", "dumbbell" to "Dumbbells", "cable" to "Cable Machine",
        "machine" to "Machines", "smith_machine" to "Smith Machine", "rack" to "Power Rack",
        "bench" to "Bench", "kettlebell" to "Kettlebell", "resistance_band" to "Resistance Bands",
        "pull_up_bar" to "Pull-up Bar", "dip_bar" to "Dip Bar", "bodyweight" to "Bodyweight Only",
        "cardio_machine" to "Cardio Equipment", "trx" to "TRX / Suspension",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Equipment") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(equipmentOptions) { (key, label) ->
                val isSelected = key in selected
                CheckboxListItem(
                    label = label,
                    checked = isSelected,
                    onToggle = { viewModel.toggle(key) }
                )
            }
        }
    }
}

@Composable
fun CheckboxListItem(label: String, checked: Boolean, onToggle: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
        modifier = Modifier.clickable(onClick = onToggle),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}
```

### `feature/settings/presentation/InjurySettingsScreen.kt`
```kotlin
@Composable
fun InjurySettingsScreen(
    onBack: () -> Unit,
    viewModel: InjurySettingsViewModel = hiltViewModel()
) {
    val selected by viewModel.selectedInjuries.collectAsStateWithLifecycle()
    val injuryOptions = MuscleGroup.entries.map { it.name to it.displayName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Injuries / Restrictions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Affected muscles will be excluded from workout recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                items(injuryOptions) { (key, label) ->
                    CheckboxListItem(label = label, checked = key in selected, onToggle = { viewModel.toggle(key) })
                }
            }
        }
    }
}
```

### `feature/settings/presentation/PriorityMusclesScreen.kt`
```kotlin
@Composable
fun PriorityMusclesScreen(
    onBack: () -> Unit,
    viewModel: PriorityMusclesViewModel = hiltViewModel()
) {
    val selected by viewModel.selectedMuscles.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Priority Muscles") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Priority muscles will be recommended first when they are recovered.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                items(MuscleGroup.entries) { muscle ->
                    CheckboxListItem(
                        label = muscle.displayName,
                        checked = muscle.name in selected,
                        onToggle = { viewModel.toggle(muscle.name) }
                    )
                }
            }
        }
    }
}
```

### ViewModels for settings sub-screens (pattern identical for all)
```kotlin
// TrainingScheduleViewModel
@HiltViewModel
class TrainingScheduleViewModel @Inject constructor(private val prefs: UserPreferencesDataStore) : ViewModel() {
    private val _selectedDays = MutableStateFlow<Set<DayOfWeek>>(emptySet())
    val selectedDays: StateFlow<Set<DayOfWeek>> = _selectedDays.asStateFlow()

    fun toggleDay(day: DayOfWeek) {
        _selectedDays.update { if (day in it) it - day else it + day }
        viewModelScope.launch {
            prefs.setPreferredWorkoutDays(_selectedDays.value.joinToString(",") { it.value.toString() })
        }
    }
}

// EquipmentSettingsViewModel
@HiltViewModel
class EquipmentSettingsViewModel @Inject constructor(private val prefs: UserPreferencesDataStore) : ViewModel() {
    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selectedEquipment: StateFlow<Set<String>> = _selected.asStateFlow()

    fun toggle(key: String) {
        _selected.update { if (key in it) it - key else it + key }
        viewModelScope.launch { prefs.setAvailableEquipment(_selected.value.joinToString(",")) }
    }
}

// InjurySettingsViewModel / PriorityMusclesViewModel — same pattern as EquipmentSettingsViewModel
```

---

## Verification
1. WeightTrackerScreen: log 3+ entries → line chart appears
2. Chart y-axis spans correct weight range
3. TrainingScheduleScreen: toggle Mon/Wed/Fri → days persist after back navigation
4. EquipmentSettings: uncheck "Cable Machine" → TodaysWorkoutService no longer recommends cable exercises
5. PriorityMuscles: set Chest priority → chest exercises appear first in TodaysWorkout recommendation
