# KOTLIN-PLAN-19.md — Achievements Screen, StreakDetail & EditProfile

## Goal
Replace three stub screens with real implementations:
1. `AchievementsScreen` — badge grid with progress, unlocked/locked states
2. `StreakDetailScreen` — calendar heatmap of workout days + streak stats
3. `EditProfileScreen` — name, photo, unit, class display with save

## Depends On
PLAN-09 (AchievementService, XPService), PLAN-12 (StreakService, ProfileScreen)

---

## 1. AchievementsScreen

### File: `feature/achievements/presentation/AchievementsScreen.kt`

Show all 8 badge types. Each badge has multiple tiers (Bronze/Silver/Gold).
Unlocked = full colour + title. Locked = greyed out + locked icon.

```kotlin
@Composable
fun AchievementsScreen(onBack: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel()) {
    val badges by viewModel.badges.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Achievements") }, ...) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(badges) { badge -> BadgeCard(badge) }
        }
    }
}

@Composable
private fun BadgeCard(badge: BadgeProgress) {
    val unlocked = badge.currentTier != null
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = "file:///android_asset/images/achievements/badge_${badge.type.assetName}.webp",
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer { alpha = if (unlocked) 1f else 0.3f },
            )
            Spacer(Modifier.height(8.dp))
            Text(badge.type.displayName, style = MaterialTheme.typography.labelMedium)
            Text(
                if (unlocked) badge.currentTier!!.name else "Locked",
                style = MaterialTheme.typography.bodySmall,
                color = if (unlocked) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            if (badge.nextThreshold != null) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { badge.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = GoldAccent,
                )
                Text(
                    "${badge.currentValue} / ${badge.nextThreshold}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}
```

### File: `feature/achievements/presentation/AchievementsViewModel.kt`

```kotlin
data class BadgeProgress(
    val type: BadgeType,
    val currentTier: BadgeTier?,       // null = not unlocked
    val currentValue: Int,
    val nextThreshold: Int?,           // null = maxed out
    val progress: Float,               // 0f–1f toward next tier
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementService: AchievementService,
    private val playerStatsRepository: PlayerStatsRepository,
) : ViewModel() {

    val badges: StateFlow<List<BadgeProgress>> = playerStatsRepository
        .getPlayerStats()
        .map { stats -> achievementService.computeAllBadgeProgress(stats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### Update `AchievementService.kt`
Add `computeAllBadgeProgress(stats: PlayerStats): List<BadgeProgress>` method.
Map each `BadgeType` → current earned tier + next threshold value.

### Add `BadgeType.assetName` extension:
```kotlin
val BadgeType.assetName: String get() = when (this) {
    BadgeType.CONSISTENCY  -> "consistency"
    BadgeType.EXPLORER     -> "explorer"
    BadgeType.MILESTONE    -> "milestone"
    BadgeType.MUSCLE_MASTER -> "musclemaster"
    BadgeType.PR_HUNTER    -> "prhunter"
    BadgeType.SPECIAL      -> "special"
    BadgeType.STRENGTH     -> "strength"
    BadgeType.VOLUME       -> "volume"
}
```

---

## 2. StreakDetailScreen

### File: `feature/streak/presentation/StreakDetailScreen.kt`

Show:
- Current streak count + start date (large hero)
- Calendar heatmap: 10×7 grid of last 70 days — green cell = workout logged, empty = rest
- Best streak ever stat
- Total active days stat

```kotlin
@Composable
fun StreakDetailScreen(onBack: () -> Unit, viewModel: StreakDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Streak") }, ...) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {

            // Hero
            item {
                Card(colors = CardDefaults.cardColors(containerColor = XPGreen.copy(alpha = 0.15f))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥", style = MaterialTheme.typography.displayMedium)
                        Text("${uiState.currentStreak}", style = MaterialTheme.typography.displayLarge, color = XPGreen)
                        Text("Day Streak", style = MaterialTheme.typography.titleMedium)
                        if (uiState.streakStartDate != null) {
                            Text(
                                "Since ${uiState.streakStartDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Stats row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip("Best Streak", "${uiState.bestStreak} days", Modifier.weight(1f))
                    StatChip("Total Days", "${uiState.totalActiveDays}", Modifier.weight(1f))
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Calendar heatmap — last 70 days in 10 rows × 7 cols
            item {
                Text("Last 70 Days", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                WorkoutCalendarHeatmap(activeDays = uiState.activeDaysSet)
            }
        }
    }
}

@Composable
private fun WorkoutCalendarHeatmap(activeDays: Set<LocalDate>) {
    val today = LocalDate.now()
    val days = (69 downTo 0).map { today.minusDays(it.toLong()) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(((70 / 7) * 20 + (70 / 7 - 1) * 4).dp),
    ) {
        items(days) { day ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (day in activeDays) XPGreen
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}
```

### File: `feature/streak/presentation/StreakDetailViewModel.kt`

```kotlin
data class StreakDetailUiState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalActiveDays: Int = 0,
    val streakStartDate: String? = null,
    val activeDaysSet: Set<LocalDate> = emptySet(),
)

@HiltViewModel
class StreakDetailViewModel @Inject constructor(
    private val streakService: StreakService,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
) : ViewModel() {

    val uiState: StateFlow<StreakDetailUiState> = combine(
        streakService.getStreakData(),
        workoutHistoryRepository.getWorkoutDates(),
    ) { streak, dates ->
        StreakDetailUiState(
            currentStreak = streak.currentStreak,
            bestStreak = streak.bestStreak,
            totalActiveDays = dates.size,
            streakStartDate = streak.streakStartDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
            activeDaysSet = dates.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreakDetailUiState())
}
```

### Add to `WorkoutHistoryRepository`:
```kotlin
fun getWorkoutDates(): Flow<List<LocalDate>>  // query distinct calledAt dates from workouts
```

### Add to `StreakService`:
```kotlin
data class StreakData(val currentStreak: Int, val bestStreak: Int, val streakStartDate: LocalDate?)
fun getStreakData(): Flow<StreakData>
```

---

## 3. EditProfileScreen

### File: `feature/profile/presentation/EditProfileScreen.kt`

Allow editing: display name, preferred unit (kg/lbs), player class shown as read-only.
Profile photo: show current Firebase photoUrl via Coil, tapping opens system photo picker.

```kotlin
@Composable
fun EditProfileScreen(onBack: () -> Unit, viewModel: EditProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.onPhotoSelected(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                actions = {
                    TextButton(onClick = { viewModel.save(); onBack() }) { Text("Save") }
                },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // Avatar
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                AsyncImage(
                    model = uiState.photoUri ?: uiState.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp).clip(CircleShape).clickable {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    contentScale = ContentScale.Crop,
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change photo",
                    modifier = Modifier.align(Alignment.BottomEnd).background(GoldAccent, CircleShape).padding(4.dp),
                    tint = Color.Black,
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onNameChange,
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            // Unit selector
            Text("Weight Unit", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("kg", "lbs").forEach { unit ->
                    FilterChip(
                        selected = uiState.unit == unit,
                        onClick = { viewModel.onUnitChange(unit) },
                        label = { Text(unit) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Player class (read-only)
            ListItem(
                headlineContent = { Text("Player Class") },
                trailingContent = {
                    Text(
                        uiState.playerClass?.displayName ?: "Not assigned",
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
    }
}
```

### File: `feature/profile/presentation/EditProfileViewModel.kt`

```kotlin
data class EditProfileUiState(
    val displayName: String = "",
    val photoUrl: String? = null,
    val photoUri: Uri? = null,
    val unit: String = "kg",
    val playerClass: PlayerClass? = null,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val user = auth.currentUser
            val unit = prefs.preferredUnit.first()
            val classStr = prefs.playerClass.first()  // add this key to DataStore
            _uiState.value = EditProfileUiState(
                displayName = user?.displayName ?: "",
                photoUrl = user?.photoUrl?.toString(),
                unit = unit,
                playerClass = classStr.takeIf { it.isNotEmpty() }?.let { enumValueOf<PlayerClass>(it) },
            )
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(displayName = name) }
    fun onUnitChange(unit: String) = _uiState.update { it.copy(unit = unit) }
    fun onPhotoSelected(uri: Uri) = _uiState.update { it.copy(photoUri = uri) }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            prefs.setUnit(state.unit)

            val user = auth.currentUser ?: return@launch
            val updates = UserProfileChangeRequest.Builder()
                .setDisplayName(state.displayName)

            // Upload photo if changed
            if (state.photoUri != null) {
                val ref = FirebaseStorage.getInstance().reference.child("avatars/${user.uid}.jpg")
                ref.putFile(state.photoUri).await()
                val downloadUrl = ref.downloadUrl.await()
                updates.setPhotoUri(downloadUrl)
            }

            user.updateProfile(updates.build()).await()
            firestore.collection("users").document(user.uid)
                .update(mapOf("displayName" to state.displayName)).await()
        }
    }
}
```

### Add `playerClass` key to `UserPreferencesDataStore`:
```kotlin
val playerClass: Flow<String> = store.data.map { it[Keys.PLAYER_CLASS] ?: "" }
suspend fun setPlayerClass(cls: String) = store.edit { it[Keys.PLAYER_CLASS] = cls }
// Keys.PLAYER_CLASS = stringPreferencesKey("player_class")
```

---

## NavGraph Wiring

Replace stubs:
```kotlin
composable<Screen.Achievements> {
    AchievementsScreen(onBack = { navController.popBackStack() })
}
composable<Screen.StreakDetail> {
    StreakDetailScreen(onBack = { navController.popBackStack() })
}
composable<Screen.EditProfile> {
    EditProfileScreen(onBack = { navController.popBackStack() })
}
```

---

## Execution Order

1. `BadgeType.assetName` extension + `AchievementService.computeAllBadgeProgress()`
2. `AchievementsViewModel` + `AchievementsScreen`
3. `WorkoutHistoryRepository.getWorkoutDates()` + `StreakService.getStreakData()`
4. `StreakDetailViewModel` + `StreakDetailScreen`
5. `UserPreferencesDataStore.playerClass` key
6. `EditProfileViewModel` + `EditProfileScreen`
7. NavGraph wiring

## Verification

1. Home → Profile → Achievements tap → badge grid shows, progress bars fill
2. Home → Profile → Streak → calendar heatmap renders last 70 days correctly
3. Home → Profile → Edit → change name → save → Profile shows updated name
4. Photo picker opens, photo uploads to Firebase Storage, avatar updates
