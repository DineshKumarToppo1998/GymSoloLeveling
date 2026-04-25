# KOTLIN-PLAN-07.md — ExercisePickerScreen with FTS5 Search

## Goal
Implement ExercisePickerScreen with Room FTS5 full-text search, filter chips (muscle group, equipment, category), and ExerciseRepository.

## Phase
Features — Phase 4 of 6. Depends on KOTLIN-PLAN-02, KOTLIN-PLAN-03.

---

## Files to Create

### `feature/exercise/data/ExerciseRepository.kt`
```kotlin
package com.example.gymlevels.feature.exercise.data

import com.example.gymlevels.core.data.local.dao.ExerciseDao
import com.example.gymlevels.core.data.local.entity.ExerciseEntity
import com.example.gymlevels.core.domain.model.Exercise
import com.example.gymlevels.core.util.ExerciseJsonLoader
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (exerciseDao.count() == 0) {
            val exercises = ExerciseJsonLoader.load(context)
            exerciseDao.insertAll(exercises.map { it.toEntity(json) })
        }
    }

    fun searchExercises(query: String): Flow<List<Exercise>> {
        val ftsQuery = if (query.isBlank()) "*" else "$query*"
        return exerciseDao.searchExercises(ftsQuery).map { list -> list.map { it.toDomain(json) } }
    }

    fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAll().map { list -> list.map { it.toDomain(json) } }

    fun getCustomExercises(): Flow<List<Exercise>> =
        exerciseDao.getCustom().map { list -> list.map { it.toDomain(json) } }

    fun getByMuscle(muscle: String): Flow<List<Exercise>> =
        exerciseDao.getByMuscle(muscle).map { list -> list.map { it.toDomain(json) } }

    fun getByEquipment(equipment: String): Flow<List<Exercise>> =
        exerciseDao.getByEquipment(equipment).map { list -> list.map { it.toDomain(json) } }

    suspend fun getById(id: String): Exercise? =
        exerciseDao.getById(id)?.toDomain(json)

    suspend fun saveCustomExercise(exercise: Exercise) =
        exerciseDao.insertAll(listOf(exercise.toEntity(json)))

    suspend fun deleteCustomExercise(exercise: Exercise) =
        exerciseDao.delete(exercise.toEntity(json))
}

// Mapping helpers
private fun Exercise.toEntity(json: Json) = ExerciseEntity(
    id = id, name = name, mainEquipment = mainEquipment,
    otherEquipment = json.encodeToString(otherEquipment),
    primaryMuscles = json.encodeToString(primaryMuscles),
    secondaryMuscles = json.encodeToString(secondaryMuscles),
    splitCategories = json.encodeToString(splitCategories),
    exerciseCounterType = exerciseCounterType,
    exerciseMechanics = exerciseMechanics, difficulty = difficulty,
    instructions = json.encodeToString(instructions),
    tips = json.encodeToString(tips), benefits = json.encodeToString(benefits),
    breathingInstructions = breathingInstructions,
    keywords = json.encodeToString(keywords),
    metabolicEquivalent = metabolicEquivalent, repSupplement = repSupplement,
    isCustom = isCustom, youtubeVideoId = youtubeVideoId,
)

private fun ExerciseEntity.toDomain(json: Json) = Exercise(
    id = id, name = name, mainEquipment = mainEquipment,
    otherEquipment = json.decodeFromString(otherEquipment),
    primaryMuscles = json.decodeFromString(primaryMuscles),
    secondaryMuscles = json.decodeFromString(secondaryMuscles),
    splitCategories = json.decodeFromString(splitCategories),
    exerciseCounterType = exerciseCounterType,
    exerciseMechanics = exerciseMechanics, difficulty = difficulty,
    instructions = json.decodeFromString(instructions),
    tips = json.decodeFromString(tips), benefits = json.decodeFromString(benefits),
    breathingInstructions = breathingInstructions,
    keywords = json.decodeFromString(keywords),
    metabolicEquivalent = metabolicEquivalent, repSupplement = repSupplement,
    isCustom = isCustom, youtubeVideoId = youtubeVideoId,
)
```

### `feature/exercise/di/ExerciseModule.kt`
```kotlin
@Module @InstallIn(SingletonComponent::class)
object ExerciseModule {
    @Provides @Singleton
    fun provideExerciseRepository(
        exerciseDao: ExerciseDao,
        @ApplicationContext context: Context,
        json: Json,
    ) = ExerciseRepository(exerciseDao, context, json)
}
```

### `feature/exercise/presentation/ExercisePickerViewModel.kt`
```kotlin
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMuscle = MutableStateFlow<String?>(null)
    private val _selectedEquipment = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow<String?>(null)

    val exercises: StateFlow<List<Exercise>> = combine(
        _searchQuery, _selectedMuscle, _selectedEquipment, _selectedCategory
    ) { query, muscle, equipment, category ->
        Triple(query, muscle ?: equipment ?: category, muscle to equipment)
    }.flatMapLatest { (query, _, filters) ->
        val (muscle, equipment) = filters
        when {
            _searchQuery.value.isNotBlank() -> exerciseRepository.searchExercises(query)
            muscle != null -> exerciseRepository.getByMuscle(muscle)
            equipment != null -> exerciseRepository.getByEquipment(equipment)
            else -> exerciseRepository.getAllExercises()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { exerciseRepository.ensureLoaded() }
    }

    fun onSearchQuery(q: String) { _searchQuery.value = q }
    fun onMuscleFilter(muscle: String?) { _selectedMuscle.value = muscle; _selectedEquipment.value = null }
    fun onEquipmentFilter(equipment: String?) { _selectedEquipment.value = equipment; _selectedMuscle.value = null }
    fun onCategoryFilter(category: String?) { _selectedCategory.value = category }
    fun clearFilters() { _selectedMuscle.value = null; _selectedEquipment.value = null; _selectedCategory.value = null }
}
```

### `feature/exercise/presentation/ExercisePickerScreen.kt`
```kotlin
@Composable
fun ExercisePickerScreen(
    onExerciseSelected: (Exercise) -> Unit,
    onBack: () -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Add Exercise") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                MuscleFilterChips(onMuscleSelected = viewModel::onMuscleFilter)
                EquipmentFilterChips(onEquipmentSelected = viewModel::onEquipmentFilter)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(exercises, key = { it.id }) { exercise ->
                ExerciseListItem(
                    exercise = exercise,
                    onClick = { onExerciseSelected(exercise) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            if (exercises.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏋️", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("No exercises found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search exercises...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotBlank()) { { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, contentDescription = null) } } } else null,
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
fun MuscleFilterChips(onMuscleSelected: (String?) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MuscleGroup.entries) { muscle ->
            FilterChip(
                selected = selected == muscle.name,
                onClick = {
                    selected = if (selected == muscle.name) null else muscle.name
                    onMuscleSelected(selected)
                },
                label = { Text(muscle.displayName) }
            )
        }
    }
}

@Composable
fun EquipmentFilterChips(onEquipmentSelected: (String?) -> Unit) {
    val equipmentOptions = listOf("barbell", "dumbbell", "cable", "machine", "bodyweight", "kettlebell", "resistance_band")
    var selected by remember { mutableStateOf<String?>(null) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(equipmentOptions) { equip ->
            FilterChip(
                selected = selected == equip,
                onClick = {
                    selected = if (selected == equip) null else equip
                    onEquipmentSelected(selected)
                },
                label = { Text(equip.replace("_", " ").replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
fun ExerciseListItem(exercise: Exercise, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(exercise.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                "${exercise.primaryMuscles.take(2).joinToString(", ")} • ${exercise.mainEquipment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            DifficultyBadge(difficulty = exercise.difficulty)
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun DifficultyBadge(difficulty: Int) {
    val (color, label) = when (difficulty) {
        1 -> MaterialTheme.colorScheme.secondary to "Easy"
        2 -> Color(0xFFFF9800) to "Med"
        else -> MaterialTheme.colorScheme.error to "Hard"
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}
```

---

## Verification
1. ExercisePickerScreen loads → 265+ exercises visible in list
2. Type "bench" → FTS5 returns bench press variants instantly
3. Muscle chip "Chest" → filters to chest exercises only
4. Equipment chip "dumbbell" → shows dumbbell exercises
5. Clear search → returns to full list
6. Tap exercise → calls `onExerciseSelected` callback (used by ActiveWorkout in PLAN-08)
