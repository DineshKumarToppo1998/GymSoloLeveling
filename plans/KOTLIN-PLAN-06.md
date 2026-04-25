# KOTLIN-PLAN-06.md — HomeScreen Dashboard & Bottom Navigation Scaffold

## Goal
Implement the main HomeScreen with all dashboard cards (streak, stats, today's workout, muscle map preview, challenges), filter chips, contact list, and the bottom navigation scaffold with 4 tabs.

## Phase
Features — Phase 4 of 6. Depends on KOTLIN-PLAN-03, KOTLIN-PLAN-09 (XP/Rank), KOTLIN-PLAN-12 (Streak). Build HomeScreen with placeholder data first, wire real data after those plans complete.

---

## Files to Create

### `feature/home/presentation/HomeViewModel.kt`
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val muscleRankRepository: MuscleRankRepository,
    private val streakRepository: StreakRepository,
    private val playerStatsRepository: PlayerStatsRepository,
    private val challengeService: ChallengeService,
    private val prefsDataStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            // Collect all data streams
            combine(
                authRepository.currentUser,
                streakRepository.currentStreak,
                playerStatsRepository.stats,
                muscleRankRepository.allRanks,
            ) { user, streak, stats, ranks ->
                HomeUiState(
                    user = user,
                    currentStreak = streak,
                    playerStats = stats,
                    muscleRanks = ranks,
                    activeChallenges = challengeService.getDailyAndWeeklyChallenges(),
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onFilterSelected(filter: HomeFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}

data class HomeUiState(
    val user: User? = null,
    val currentStreak: Int = 0,
    val playerStats: PlayerStats? = null,
    val muscleRanks: List<MuscleRank> = emptyList(),
    val activeChallenges: List<Challenge> = emptyList(),
    val activeFilter: HomeFilter = HomeFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null,
)

enum class HomeFilter { ALL, OVERDUE, WEEKLY, MONTHLY }
```

### `feature/home/presentation/HomeScreen.kt`
```kotlin
@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    onMuscleRankings: () -> Unit,
    onChallenges: () -> Unit,
    onProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("GymLevels", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartWorkout,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Start Workout") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Streak Banner
            item {
                StreakBannerCard(
                    streak = uiState.currentStreak,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Stats row
            item {
                uiState.playerStats?.let { stats ->
                    PlayerStatsRow(
                        stats = stats,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // XP Progress bar
            item {
                uiState.playerStats?.let { stats ->
                    XPProgressCard(stats = stats, modifier = Modifier.padding(16.dp))
                }
            }

            // Filter chips
            item {
                HomeFilterChips(
                    activeFilter = uiState.activeFilter,
                    onFilterSelected = viewModel::onFilterSelected,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Active Challenges teaser
            if (uiState.activeChallenges.isNotEmpty()) {
                item {
                    ChallengesTeaserCard(
                        challenges = uiState.activeChallenges.take(2),
                        onClick = onChallenges,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Muscle Rankings preview
            item {
                MuscleRankingsPreviewCard(
                    ranks = uiState.muscleRanks,
                    onClick = onMuscleRankings,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun StreakBannerCard(streak: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = streak.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Day Streak", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 6.dp))
                }
                if (streak > 0) {
                    Text("Keep the momentum going!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    Text("Start your streak today!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Text("🔥", style = MaterialTheme.typography.displaySmall)
        }
    }
}

@Composable
fun PlayerStatsRow(stats: PlayerStats, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(label = "Level", value = stats.playerLevel.toString(), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        StatCard(label = "Workouts", value = stats.totalWorkouts.toString(), color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
        StatCard(label = "PRs", value = stats.totalPRs.toString(), color = GoldAccent, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun XPProgressCard(stats: PlayerStats, modifier: Modifier = Modifier) {
    val thresholds = XPThresholds.PLAYER_LEVEL_THRESHOLDS
    val currentLevelXp = thresholds.getOrNull(stats.playerLevel - 1) ?: 0
    val nextLevelXp = thresholds.getOrNull(stats.playerLevel) ?: (currentLevelXp + 1000)
    val progress = ((stats.totalXp - currentLevelXp).coerceAtLeast(0).toFloat() / (nextLevelXp - currentLevelXp)).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Level ${stats.playerLevel}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text("${stats.totalXp} XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
            )
            Text("${nextLevelXp - stats.totalXp} XP to Level ${stats.playerLevel + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun HomeFilterChips(activeFilter: HomeFilter, onFilterSelected: (HomeFilter) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(HomeFilter.entries) { filter ->
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
fun ChallengesTeaserCard(challenges: List<Challenge>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Active Challenges", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("See all →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            challenges.forEach { challenge ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(challenge.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("+${challenge.xpReward} XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { (challenge.currentValue / challenge.targetValue).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
fun MuscleRankingsPreviewCard(ranks: List<MuscleRank>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Muscle Rankings", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                val highest = ranks.maxByOrNull { it.totalXp }
                highest?.let {
                    Text("Best: ${it.muscleGroup.displayName} — ${it.currentRank.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}
```

### `core/navigation/MainScaffold.kt`
```kotlin
@Composable
fun MainScaffold(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute(item.screen::class, null) } == true,
                        onClick = {
                            navController.navigate(item.screen) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        content = content
    )
}

enum class BottomNavItem(val label: String, val icon: ImageVector, val screen: Screen) {
    HOME("Home", Icons.Default.Home, Screen.Home),
    HISTORY("History", Icons.Default.FitnessCenter, Screen.WorkoutHistory),
    RANKINGS("Rankings", Icons.Default.Leaderboard, Screen.MuscleRankings),
    PROFILE("Profile", Icons.Default.Person, Screen.Profile),
}
```

---

## Verification
1. App launches → HomeScreen visible with bottom navigation (4 tabs)
2. Each tab navigates correctly with back-stack preservation
3. StreakBannerCard, PlayerStatsRow, XPProgressCard render with mock/default data
4. Filter chips respond to tap selection
5. MuscleRankingsPreviewCard tap navigates to MuscleRankings (stub)
