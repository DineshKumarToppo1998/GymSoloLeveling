# KOTLIN-PLAN-20.md — WeightTracker, TrainingSchedule, EquipmentSettings, InjurySettings & PriorityMuscles

## Goal
Replace five PLAN-16 stub screens with full implementations. All use images from
`assets/images/onboarding/` already copied to the project.

## Depends On
PLAN-02 (Room DB), PLAN-03 (DataStore, enums), PLAN-16 screens defined in NavGraph

---

## 1. WeightTrackerScreen

### New Room Entity: `core/data/local/entity/WeightEntryEntity.kt`
```kotlin
@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val weightKg: Double,
    val loggedAt: Long,   // epoch millis
    val notes: String? = null,
)
```

Add to `GymLevelsDatabase` entities list and increment version to 2 with migration:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS weight_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uid TEXT NOT NULL, weightKg REAL NOT NULL, loggedAt INTEGER NOT NULL, notes TEXT)")
    }
}
```

### New DAO: `core/data/local/dao/WeightEntryDao.kt`
```kotlin
@Dao
interface WeightEntryDao {
    @Query("SELECT * FROM weight_entries WHERE uid = :uid ORDER BY loggedAt DESC")
    fun getAll(uid: String): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries WHERE uid = :uid ORDER BY loggedAt ASC LIMIT 90")
    suspend fun getLast90(uid: String): List<WeightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntryEntity)

    @Delete
    suspend fun delete(entry: WeightEntryEntity)
}
```

Add to `DatabaseModule` and `GymLevelsDatabase`.

### File: `feature/weight/data/WeightRepository.kt`
```kotlin
@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightEntryDao,
    private val sessionManager: SessionManager,
) {
    fun getEntries(): Flow<List<WeightEntryEntity>> {
        val uid = sessionManager.session.value.effectiveUserId
        return dao.getAll(uid)
    }

    suspend fun logWeight(weightKg: Double, notes: String?) {
        val uid = sessionManager.session.value.effectiveUserId
        dao.insert(WeightEntryEntity(uid = uid, weightKg = weightKg, loggedAt = System.currentTimeMillis(), notes = notes))
    }

    suspend fun deleteEntry(entry: WeightEntryEntity) = dao.delete(entry)
}
```

### File: `feature/weight/presentation/WeightTrackerViewModel.kt`
```kotlin
data class WeightTrackerUiState(
    val entries: List<WeightEntryEntity> = emptyList(),
    val showAddDialog: Boolean = false,
    val inputWeight: String = "",
    val inputNotes: String = "",
    val unit: String = "kg",
)

@HiltViewModel
class WeightTrackerViewModel @Inject constructor(
    private val weightRepository: WeightRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeightTrackerUiState())
    val uiState: StateFlow<WeightTrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(weightRepository.getEntries(), prefs.preferredUnit) { entries, unit ->
                _uiState.update { it.copy(entries = entries, unit = unit) }
            }.collect()
        }
    }

    fun showDialog() = _uiState.update { it.copy(showAddDialog = true) }
    fun hideDialog() = _uiState.update { it.copy(showAddDialog = false, inputWeight = "", inputNotes = "") }
    fun onWeightInput(w: String) = _uiState.update { it.copy(inputWeight = w) }
    fun onNotesInput(n: String) = _uiState.update { it.copy(inputNotes = n) }

    fun logWeight() {
        val kg = _uiState.value.inputWeight.toDoubleOrNull() ?: return
        val actualKg = if (_uiState.value.unit == "lbs") kg * 0.453592 else kg
        viewModelScope.launch {
            weightRepository.logWeight(actualKg, _uiState.value.inputNotes.takeIf { it.isNotBlank() })
            hideDialog()
        }
    }

    fun deleteEntry(entry: WeightEntryEntity) {
        viewModelScope.launch { weightRepository.deleteEntry(entry) }
    }
}
```

### File: `feature/weight/presentation/WeightTrackerScreen.kt`

Key UI elements:
- **Canvas line chart**: draw path through sorted weight entries, date on X-axis, weight on Y-axis
- **Log entry list**: LazyColumn below chart, each row has date + weight + delete swipe
- **FAB**: "Log Weight" opens dialog

```kotlin
@Composable
fun WeightTrackerScreen(onBack: () -> Unit, viewModel: WeightTrackerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Weight Tracker") }, ...) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showDialog, containerColor = GoldAccent) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (uiState.entries.size >= 2) {
                WeightLineChart(
                    entries = uiState.entries.sortedBy { it.loggedAt },
                    unit = uiState.unit,
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp),
                )
            }

            LazyColumn {
                items(uiState.entries, key = { it.id }) { entry ->
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { viewModel.deleteEntry(entry); true } else false }
                        ),
                        backgroundContent = {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(16.dp))
                            }
                        },
                    ) {
                        ListItem(
                            headlineContent = {
                                val display = if (uiState.unit == "lbs") entry.weightKg / 0.453592 else entry.weightKg
                                Text("%.1f ${uiState.unit}".format(display))
                            },
                            supportingContent = {
                                Text(Instant.ofEpochMilli(entry.loggedAt).atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                            },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideDialog,
            title = { Text("Log Weight") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.inputWeight,
                        onValueChange = viewModel::onWeightInput,
                        label = { Text("Weight (${uiState.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = uiState.inputNotes,
                        onValueChange = viewModel::onNotesInput,
                        label = { Text("Notes (optional)") },
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::logWeight) { Text("Log") } },
            dismissButton = { TextButton(onClick = viewModel::hideDialog) { Text("Cancel") } },
        )
    }
}

// Canvas line chart
@Composable
private fun WeightLineChart(entries: List<WeightEntryEntity>, unit: String, modifier: Modifier) {
    val lineColor = XPGreen
    val textColor = Color.Gray

    Canvas(modifier = modifier) {
        val weights = entries.map { if (unit == "lbs") it.weightKg / 0.453592 else it.weightKg }
        val minW = weights.min()
        val maxW = weights.max()
        val range = (maxW - minW).coerceAtLeast(1.0)

        val points = entries.mapIndexed { i, _ ->
            val x = i / (entries.size - 1).toFloat() * size.width
            val y = (1f - ((weights[i] - minW) / range).toFloat()) * size.height
            Offset(x, y)
        }

        // Draw line path
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // Draw dots
        points.forEach { drawCircle(lineColor, radius = 5.dp.toPx(), center = it) }
    }
}
```

---

## 2. TrainingScheduleScreen

### File: `feature/settings/presentation/TrainingScheduleScreen.kt`

Select which days of the week the user trains.
Persisted to `DataStore.preferredWorkoutDays` (comma-separated: "MON,WED,FRI").

```kotlin
@Composable
fun TrainingScheduleScreen(onBack: () -> Unit, viewModel: TrainingScheduleViewModel = hiltViewModel()) {
    val selected by viewModel.selectedDays.collectAsStateWithLifecycle()

    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    Scaffold(topBar = { TopAppBar(title = { Text("Training Schedule") }, ...) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Select your training days", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Spacer(Modifier.height(24.dp))

            // 7-cell row — day abbreviations as toggle buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                days.forEach { day ->
                    val isSelected = day in selected
                    Surface(
                        onClick = { viewModel.toggleDay(day) },
                        shape = CircleShape,
                        color = if (isSelected) XPGreen else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                day.take(1),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "${selected.size} days/week selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
```

### File: `feature/settings/presentation/TrainingScheduleViewModel.kt`
```kotlin
@HiltViewModel
class TrainingScheduleViewModel @Inject constructor(private val prefs: UserPreferencesDataStore) : ViewModel() {
    val selectedDays: StateFlow<Set<String>> = prefs.preferredWorkoutDays
        .map { it.split(",").filter { d -> d.isNotBlank() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleDay(day: String) {
        viewModelScope.launch {
            val current = selectedDays.value.toMutableSet()
            if (!current.add(day)) current.remove(day)
            prefs.setPreferredWorkoutDays(current.joinToString(","))
        }
    }
}
```

---

## 3. EquipmentSettingsScreen

### File: `feature/settings/presentation/EquipmentSettingsScreen.kt`

Multi-select grid of equipment with WebP images from `assets/images/onboarding/equipment/`.

```kotlin
private val EQUIPMENT_OPTIONS = listOf(
    "full_gym"          to "Full Gym",
    "barbell_plates"    to "Barbell & Plates",
    "dumbbells"         to "Dumbbells",
    "squat_rack"        to "Squat Rack",
    "bench"             to "Bench",
    "cable_machine"     to "Cable Machine",
    "gym_machines"      to "Gym Machines",
    "kettlebells"       to "Kettlebells",
    "pull_up_bar"       to "Pull-Up Bar",
    "resistance_bands"  to "Resistance Bands",
    "exercise_ball"     to "Exercise Ball",
    "bodyweight_only"   to "Bodyweight Only",
    "cardio_machines"   to "Cardio Machines",
)

@Composable
fun EquipmentSettingsScreen(onBack: () -> Unit, viewModel: EquipmentSettingsViewModel = hiltViewModel()) {
    val selected by viewModel.selectedEquipment.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Equipment") }, ...) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp) + PaddingValues(top = padding.calculateTopPadding()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(EQUIPMENT_OPTIONS) { (id, label) ->
                val isSelected = id in selected
                Card(
                    onClick = { viewModel.toggle(id) },
                    border = if (isSelected) BorderStroke(2.dp, XPGreen) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) XPGreen.copy(alpha = 0.1f)
                                         else MaterialTheme.colorScheme.surfaceVariant
                    ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    ) {
                        AsyncImage(
                            model = "file:///android_asset/images/onboarding/equipment/$id.webp",
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
```

### File: `feature/settings/presentation/EquipmentSettingsViewModel.kt`
```kotlin
@HiltViewModel
class EquipmentSettingsViewModel @Inject constructor(private val prefs: UserPreferencesDataStore) : ViewModel() {
    val selectedEquipment: StateFlow<Set<String>> = prefs.availableEquipment
        .map { it.split(",").filter { e -> e.isNotBlank() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(id: String) {
        viewModelScope.launch {
            val current = selectedEquipment.value.toMutableSet()
            if (!current.add(id)) current.remove(id)
            prefs.setAvailableEquipment(current.joinToString(","))
        }
    }
}
```

---

## 4. InjurySettingsScreen

### File: `feature/settings/presentation/InjurySettingsScreen.kt`

Single-select (or multi-select with "None" exclusive). Images from `assets/images/onboarding/injuries/`.

```kotlin
private val INJURY_OPTIONS = listOf(
    "none"         to "No Injuries",
    "lower_back"   to "Lower Back",
    "knee"         to "Knee",
    "shoulder"     to "Shoulder",
    "wrist_elbow"  to "Wrist / Elbow",
    "hip"          to "Hip",
)
```

Add `injuries` key to `UserPreferencesDataStore`:
```kotlin
val activeInjuries: Flow<String> = store.data.map { it[Keys.INJURIES] ?: "" }
suspend fun setInjuries(json: String) = store.edit { it[Keys.INJURIES] = json }
// Keys.INJURIES = stringPreferencesKey("injuries")
```

Same card-grid pattern as EquipmentSettings, but selecting "none" clears all others;
selecting any injury clears "none".

### File: `feature/settings/presentation/InjurySettingsViewModel.kt`
Same pattern as `EquipmentSettingsViewModel` but with exclusive "none" logic.

---

## 5. PriorityMusclesScreen

### File: `feature/settings/presentation/PriorityMusclesScreen.kt`

Let user pick up to 3 priority muscle groups. Shown in TodaysWorkoutService to bias recommendations.

```kotlin
@Composable
fun PriorityMusclesScreen(onBack: () -> Unit, viewModel: PriorityMusclesViewModel = hiltViewModel()) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Priority Muscles") }, ...) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "Pick up to 3 muscle groups to prioritise in your training plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(columns = GridCells.Fixed(3), ...) {
                items(MuscleGroup.entries) { muscle ->
                    val isSelected = muscle.name in selected
                    val canSelect = isSelected || selected.size < 3
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (canSelect || isSelected) viewModel.toggle(muscle.name) },
                        label = { Text(muscle.displayName, style = MaterialTheme.typography.labelSmall) },
                        enabled = canSelect || isSelected,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                            selectedLabelColor = GoldAccent,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "${selected.size}/3 selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
```

### File: `feature/settings/presentation/PriorityMusclesViewModel.kt`
```kotlin
@HiltViewModel
class PriorityMusclesViewModel @Inject constructor(private val prefs: UserPreferencesDataStore) : ViewModel() {
    val selected: StateFlow<Set<String>> = prefs.priorityMuscles
        .map { it.split(",").filter { m -> m.isNotBlank() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(muscleName: String) {
        viewModelScope.launch {
            val current = selected.value.toMutableSet()
            if (!current.add(muscleName)) current.remove(muscleName)
            if (current.size <= 3) prefs.setPriorityMuscles(current.joinToString(","))
        }
    }
}
```

---

## NavGraph Wiring

Replace all five stubs:
```kotlin
composable<Screen.WeightTracker> {
    WeightTrackerScreen(onBack = { navController.popBackStack() })
}
composable<Screen.TrainingSchedule> {
    TrainingScheduleScreen(onBack = { navController.popBackStack() })
}
composable<Screen.EquipmentSettings> {
    EquipmentSettingsScreen(onBack = { navController.popBackStack() })
}
composable<Screen.InjurySettings> {
    InjurySettingsScreen(onBack = { navController.popBackStack() })
}
composable<Screen.PriorityMuscles> {
    PriorityMusclesScreen(onBack = { navController.popBackStack() })
}
```

---

## Execution Order

1. `WeightEntryEntity` + DAO + DB migration + WeightRepository
2. `WeightTrackerViewModel` + `WeightTrackerScreen`
3. `TrainingScheduleViewModel` + `TrainingScheduleScreen`
4. `EquipmentSettingsViewModel` + `EquipmentSettingsScreen`
5. DataStore `injuries` key + `InjurySettingsViewModel` + `InjurySettingsScreen`
6. `PriorityMusclesViewModel` + `PriorityMusclesScreen`
7. NavGraph wiring all five
8. `./gradlew assembleDebug` — verify no errors

## Verification

1. Settings → Weight Tracker → log 3 entries → chart renders line between points
2. Settings → Training Schedule → toggle days → re-open → state persists
3. Settings → Equipment → select 3 items → re-open → selections persist
4. Settings → Injuries → tap "Shoulder" → "None" deselects, vice versa
5. Settings → Priority Muscles → try selecting 4th → blocked at 3
