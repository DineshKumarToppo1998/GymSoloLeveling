# KOTLIN-PLAN-10.md — MuscleBodyView (Canvas) & MuscleRankingsScreen

## Goal
Implement the interactive muscle body diagram using Compose Canvas with normalized Path objects for 23 muscle regions (12 front, 11 back), and the MuscleRankingsScreen showing per-muscle XP and rank tiers.

## Phase
Features — Phase 4 of 6. Depends on KOTLIN-PLAN-09.

---

## Architecture Notes
- 23 painters: 12 front body regions, 11 back body regions
- Paths are normalized to 0.0–1.0 coordinate space, scaled to canvas size at draw time
- Tap detection: find which path contains the tap point
- Color each region by its RankTier color

---

## Files to Create

### `feature/rankings/data/MuscleRankRepository.kt`
```kotlin
@Singleton
class MuscleRankRepository @Inject constructor(
    private val muscleRankDao: MuscleRankDao,
    private val firestore: FirebaseFirestore,
) {
    val allRanks: Flow<List<MuscleRank>> = flow {
        val entities = muscleRankDao.getAll()
        val ranks = MuscleGroup.entries.map { muscle ->
            val entity = entities.firstOrNull { it.muscleGroup.equals(muscle.name, ignoreCase = true) }
            MuscleRank(
                muscleGroup = muscle,
                totalXp = entity?.totalXp ?: 0,
                currentRank = entity?.currentRank?.let { runCatching { RankTier.valueOf(it) }.getOrDefault(RankTier.UNTRAINED) } ?: RankTier.UNTRAINED,
                currentSubRank = entity?.currentSubRank?.let { runCatching { RankSubTier.valueOf(it) }.getOrNull() },
                xpToNextRank = entity?.xpToNextRank ?: 50,
            )
        }
        emit(ranks)
    }
}
```

### `feature/rankings/presentation/MuscleRankingsViewModel.kt`
```kotlin
@HiltViewModel
class MuscleRankingsViewModel @Inject constructor(
    private val muscleRankRepository: MuscleRankRepository,
) : ViewModel() {

    val ranks: StateFlow<List<MuscleRank>> = muscleRankRepository.allRanks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMuscle = MutableStateFlow<MuscleRank?>(null)
    val selectedMuscle: StateFlow<MuscleRank?> = _selectedMuscle.asStateFlow()

    fun onMuscleSelected(muscle: MuscleRank) {
        _selectedMuscle.value = if (_selectedMuscle.value == muscle) null else muscle
    }
}
```

### `feature/rankings/presentation/MusclePaths.kt`
```kotlin
package com.example.gymlevels.feature.rankings.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.example.gymlevels.core.domain.model.enums.MuscleGroup

/**
 * Normalized muscle paths for a standard body silhouette.
 * Coordinates are in 0.0–1.0 space (width × height).
 * Front body viewBox: 0 0 200 400
 * Back body viewBox: 0 0 200 400
 */
object MusclePaths {

    data class MusclePathData(
        val muscle: MuscleGroup,
        val path: Path,
        val isFront: Boolean,
    )

    // Front body paths (normalized to 200×400 viewBox)
    fun buildFrontPaths(): List<MusclePathData> = listOf(
        // Chest
        MusclePathData(MuscleGroup.CHEST, Path().apply {
            moveTo(70f, 110f); lineTo(100f, 115f); lineTo(130f, 110f)
            lineTo(135f, 145f); lineTo(100f, 155f); lineTo(65f, 145f); close()
        }, isFront = true),

        // Front Shoulders (left)
        MusclePathData(MuscleGroup.FRONT_SHOULDERS, Path().apply {
            moveTo(55f, 100f); lineTo(70f, 100f); lineTo(72f, 130f)
            lineTo(55f, 125f); close()
        }, isFront = true),

        // Front Shoulders (right)  — same muscle group, drawn twice
        MusclePathData(MuscleGroup.FRONT_SHOULDERS, Path().apply {
            moveTo(130f, 100f); lineTo(145f, 100f); lineTo(145f, 125f)
            lineTo(128f, 130f); close()
        }, isFront = true),

        // Biceps (left)
        MusclePathData(MuscleGroup.BICEPS, Path().apply {
            moveTo(48f, 130f); lineTo(60f, 130f); lineTo(62f, 165f)
            lineTo(48f, 160f); close()
        }, isFront = true),

        // Biceps (right)
        MusclePathData(MuscleGroup.BICEPS, Path().apply {
            moveTo(140f, 130f); lineTo(152f, 130f); lineTo(152f, 160f)
            lineTo(138f, 165f); close()
        }, isFront = true),

        // Abs
        MusclePathData(MuscleGroup.ABS, Path().apply {
            moveTo(82f, 155f); lineTo(118f, 155f); lineTo(118f, 210f)
            lineTo(82f, 210f); close()
        }, isFront = true),

        // Obliques (left)
        MusclePathData(MuscleGroup.OBLIQUES, Path().apply {
            moveTo(68f, 155f); lineTo(82f, 158f); lineTo(80f, 210f)
            lineTo(65f, 200f); close()
        }, isFront = true),

        // Obliques (right)
        MusclePathData(MuscleGroup.OBLIQUES, Path().apply {
            moveTo(118f, 158f); lineTo(132f, 155f); lineTo(135f, 200f)
            lineTo(120f, 210f); close()
        }, isFront = true),

        // Quadriceps (left)
        MusclePathData(MuscleGroup.QUADRICEPS, Path().apply {
            moveTo(72f, 220f); lineTo(98f, 220f); lineTo(95f, 295f)
            lineTo(68f, 290f); close()
        }, isFront = true),

        // Quadriceps (right)
        MusclePathData(MuscleGroup.QUADRICEPS, Path().apply {
            moveTo(102f, 220f); lineTo(128f, 220f); lineTo(132f, 290f)
            lineTo(105f, 295f); close()
        }, isFront = true),

        // Calves (left)
        MusclePathData(MuscleGroup.CALVES, Path().apply {
            moveTo(73f, 305f); lineTo(92f, 305f); lineTo(90f, 365f)
            lineTo(72f, 360f); close()
        }, isFront = true),

        // Calves (right)
        MusclePathData(MuscleGroup.CALVES, Path().apply {
            moveTo(108f, 305f); lineTo(127f, 305f); lineTo(128f, 360f)
            lineTo(110f, 365f); close()
        }, isFront = true),
    )

    // Back body paths
    fun buildBackPaths(): List<MusclePathData> = listOf(
        // Back Shoulders
        MusclePathData(MuscleGroup.BACK_SHOULDERS, Path().apply {
            moveTo(55f, 100f); lineTo(72f, 100f); lineTo(72f, 128f)
            lineTo(55f, 122f); close()
        }, isFront = false),
        MusclePathData(MuscleGroup.BACK_SHOULDERS, Path().apply {
            moveTo(128f, 100f); lineTo(145f, 100f); lineTo(145f, 122f)
            lineTo(128f, 128f); close()
        }, isFront = false),

        // Traps
        MusclePathData(MuscleGroup.TRAPS, Path().apply {
            moveTo(80f, 88f); lineTo(100f, 82f); lineTo(120f, 88f)
            lineTo(128f, 108f); lineTo(100f, 112f); lineTo(72f, 108f); close()
        }, isFront = false),

        // Upper Back
        MusclePathData(MuscleGroup.UPPER_BACK, Path().apply {
            moveTo(72f, 112f); lineTo(128f, 112f); lineTo(128f, 148f)
            lineTo(72f, 148f); close()
        }, isFront = false),

        // Lats (left)
        MusclePathData(MuscleGroup.LATS, Path().apply {
            moveTo(62f, 125f); lineTo(72f, 125f); lineTo(74f, 175f)
            lineTo(60f, 165f); close()
        }, isFront = false),

        // Lats (right)
        MusclePathData(MuscleGroup.LATS, Path().apply {
            moveTo(128f, 125f); lineTo(138f, 125f); lineTo(140f, 165f)
            lineTo(126f, 175f); close()
        }, isFront = false),

        // Middle Back
        MusclePathData(MuscleGroup.MIDDLE_BACK, Path().apply {
            moveTo(78f, 148f); lineTo(122f, 148f); lineTo(120f, 175f)
            lineTo(80f, 175f); close()
        }, isFront = false),

        // Lower Back
        MusclePathData(MuscleGroup.LOWER_BACK, Path().apply {
            moveTo(82f, 175f); lineTo(118f, 175f); lineTo(116f, 205f)
            lineTo(84f, 205f); close()
        }, isFront = false),

        // Glutes
        MusclePathData(MuscleGroup.GLUTES, Path().apply {
            moveTo(72f, 205f); lineTo(128f, 205f); lineTo(130f, 240f)
            lineTo(70f, 240f); close()
        }, isFront = false),

        // Hamstrings (left)
        MusclePathData(MuscleGroup.HAMSTRINGS, Path().apply {
            moveTo(72f, 242f); lineTo(97f, 242f); lineTo(94f, 295f)
            lineTo(68f, 290f); close()
        }, isFront = false),

        // Hamstrings (right)
        MusclePathData(MuscleGroup.HAMSTRINGS, Path().apply {
            moveTo(103f, 242f); lineTo(128f, 242f); lineTo(132f, 290f)
            lineTo(106f, 295f); close()
        }, isFront = false),
    )

    fun hitTest(paths: List<MusclePathData>, tapX: Float, tapY: Float, canvasWidth: Float, canvasHeight: Float): MuscleGroup? {
        val viewBoxW = 200f
        val viewBoxH = 400f
        val scaleX = canvasWidth / viewBoxW
        val scaleY = canvasHeight / viewBoxH

        paths.forEach { pathData ->
            val scaledPath = Path().apply {
                addPath(pathData.path, Offset.Zero)
                transform(androidx.compose.ui.graphics.Matrix().apply {
                    scale(scaleX, scaleY)
                })
            }
            // Approximated bounds hit test (full contains() requires Path.op which is costly)
            val bounds = androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight)
            if (bounds.contains(Offset(tapX, tapY))) {
                // Simple bounding box approximation
                val pathBounds = scaledPath.getBounds()
                if (pathBounds.contains(Offset(tapX, tapY))) {
                    return pathData.muscle
                }
            }
        }
        return null
    }
}
```

### `feature/rankings/presentation/MuscleBodyView.kt`
```kotlin
@Composable
fun MuscleBodyView(
    muscleRanks: List<MuscleRank>,
    isFrontView: Boolean,
    onMuscleSelected: (MuscleGroup) -> Unit,
    selectedMuscle: MuscleGroup? = null,
    modifier: Modifier = Modifier,
) {
    val rankMap = muscleRanks.associateBy { it.muscleGroup }
    val paths = remember(isFrontView) {
        if (isFrontView) MusclePaths.buildFrontPaths()
        else MusclePaths.buildBackPaths()
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.5f)
            .pointerInput(isFrontView) {
                detectTapGestures { offset ->
                    val muscle = MusclePaths.hitTest(
                        paths = paths,
                        tapX = offset.x, tapY = offset.y,
                        canvasWidth = size.width.toFloat(),
                        canvasHeight = size.height.toFloat()
                    )
                    muscle?.let { onMuscleSelected(it) }
                }
            }
    ) {
        val viewBoxW = 200f
        val viewBoxH = 400f
        val scaleX = size.width / viewBoxW
        val scaleY = size.height / viewBoxH

        // Draw body silhouette background
        drawRoundRect(
            color = Color(0xFF2A2A2A),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(16f * scaleX)
        )

        paths.forEach { pathData ->
            val rank = rankMap[pathData.muscle]
            val baseColor = RankService.getRankColor(rank?.currentRank ?: RankTier.UNTRAINED)
            val isSelected = selectedMuscle == pathData.muscle

            withTransform({ scale(scaleX, scaleY, pivot = Offset.Zero) }) {
                drawPath(
                    path = pathData.path,
                    color = if (isSelected) baseColor else baseColor.copy(alpha = 0.7f),
                )
                if (isSelected) {
                    drawPath(
                        path = pathData.path,
                        color = Color.White,
                        style = Stroke(width = 2f / scaleX)
                    )
                }
            }
        }
    }
}
```

### `feature/rankings/presentation/MuscleRankingsScreen.kt`
```kotlin
@Composable
fun MuscleRankingsScreen(
    onBack: () -> Unit,
    viewModel: MuscleRankingsViewModel = hiltViewModel()
) {
    val ranks by viewModel.ranks.collectAsStateWithLifecycle()
    val selectedMuscle by viewModel.selectedMuscle.collectAsStateWithLifecycle()
    var isFrontView by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Muscle Rankings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Front / Back toggle
            SegmentedButton(
                options = listOf("Front", "Back"),
                selectedIndex = if (isFrontView) 0 else 1,
                onSelected = { isFrontView = it == 0 },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Body diagram
                MuscleBodyView(
                    muscleRanks = ranks,
                    isFrontView = isFrontView,
                    onMuscleSelected = { muscle ->
                        ranks.firstOrNull { it.muscleGroup == muscle }?.let { viewModel.onMuscleSelected(it) }
                    },
                    selectedMuscle = selectedMuscle?.muscleGroup,
                    modifier = Modifier.weight(1f)
                )

                // Rank list
                LazyColumn(Modifier.weight(1f)) {
                    val visibleRanks = if (isFrontView) {
                        listOf(MuscleGroup.CHEST, MuscleGroup.FRONT_SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.ABS, MuscleGroup.OBLIQUES, MuscleGroup.QUADRICEPS, MuscleGroup.CALVES)
                    } else {
                        listOf(MuscleGroup.BACK_SHOULDERS, MuscleGroup.TRAPS, MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.MIDDLE_BACK, MuscleGroup.LOWER_BACK, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)
                    }

                    items(visibleRanks) { muscleGroup ->
                        val rank = ranks.firstOrNull { it.muscleGroup == muscleGroup }
                        MuscleRankRow(
                            muscleGroup = muscleGroup,
                            rank = rank,
                            isSelected = selectedMuscle?.muscleGroup == muscleGroup,
                            onClick = {
                                rank?.let { viewModel.onMuscleSelected(it) }
                            }
                        )
                    }
                }
            }

            // Selected muscle detail card
            AnimatedVisibility(visible = selectedMuscle != null) {
                selectedMuscle?.let { rank ->
                    Spacer(Modifier.height(16.dp))
                    MuscleDetailCard(rank = rank)
                }
            }
        }
    }
}

@Composable
fun MuscleRankRow(muscleGroup: MuscleGroup, rank: MuscleRank?, isSelected: Boolean, onClick: () -> Unit) {
    val rankTier = rank?.currentRank ?: RankTier.UNTRAINED
    val rankColor = RankService.getRankColor(rankTier)

    Surface(
        onClick = onClick,
        color = if (isSelected) rankColor.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).background(rankColor, CircleShape))
            Text(muscleGroup.displayName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(
                RankService.getRankDisplayName(rankTier, rank?.currentSubRank),
                style = MaterialTheme.typography.labelSmall,
                color = rankColor
            )
        }
    }
}

@Composable
fun MuscleDetailCard(rank: MuscleRank) {
    val rankColor = RankService.getRankColor(rank.currentRank)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rank.muscleGroup.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    RankService.getRankDisplayName(rank.currentRank, rank.currentSubRank),
                    style = MaterialTheme.typography.titleSmall,
                    color = rankColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("${rank.totalXp} XP total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            // Progress to next rank
            if (rank.currentRank != RankTier.LEGEND) {
                LinearProgressIndicator(
                    progress = {
                        val nextThreshold = XPThresholds.SUB_RANK_THRESHOLDS.firstOrNull { it.third > rank.totalXp }?.third?.toFloat() ?: (rank.totalXp.toFloat() + 1)
                        val prevThreshold = XPThresholds.SUB_RANK_THRESHOLDS.lastOrNull { it.third <= rank.totalXp }?.third?.toFloat() ?: 0f
                        ((rank.totalXp - prevThreshold) / (nextThreshold - prevThreshold)).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = rankColor,
                )
                Text("${rank.xpToNextRank} XP to next rank", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                Text("Maximum rank achieved!", style = MaterialTheme.typography.bodyMedium, color = rankColor)
            }
        }
    }
}

@Composable
fun SegmentedButton(options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
```

---

## Verification
1. MuscleRankingsScreen opens → body diagram visible with colored muscle regions
2. Tapping a muscle region highlights it and shows detail card below
3. Front/Back toggle switches body view
4. After completing a chest workout: chest region changes from gray to bronze color
5. Rank list on right side updates in real-time with correct rank labels
