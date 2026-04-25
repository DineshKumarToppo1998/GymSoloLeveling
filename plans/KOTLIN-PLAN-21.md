# KOTLIN-PLAN-21.md — Onboarding Quiz: Image Cards + Full 8-Step Flow + PlayerClass Logic

## Goal
Upgrade shallow OnboardingQuiz to match APK fidelity:
- Full 8 steps (currently 5)
- Each option shows APK asset image (full-bleed card) instead of emoji text
- Smooth HorizontalPager with progress bar
- Improved `PlayerClassAssigner` mapping all 8 answers → `PlayerClass`
- `PlayerClassRevealScreen` uses class artwork + animated reveal

## Depends On
PLAN-05 (OnboardingQuizScreen, OnboardingViewModel, PlayerClassRevealScreen)

---

## Step Definitions (match APK exactly)

| # | Question | Asset folder | Options |
|---|---|---|---|
| 0 | Primary goal | `goals/` | build_muscle, get_stronger, lose_fat, improve_health |
| 1 | Training experience | emoji only | beginner, intermediate, advanced, veteran |
| 2 | Training days/week | emoji only | 1_2, 3_4, 5_6, 7 |
| 3 | Training style | emoji only | powerlifting, hypertrophy, hiit, functional |
| 4 | Available equipment | `equipment/` | full_gym, barbell_plates, dumbbells, bodyweight_only |
| 5 | Target physique | `physiques/` (male/female variant) | lean_toned, muscular_defined, balanced_functional, strong_powerful |
| 6 | Injuries / limitations | `injuries/` | none, lower_back, knee, shoulder, wrist_elbow, hip |
| 7 | Motivation style | `motivation/` | visual_results, data_driven, feeling_strong, competitive |

---

## File: `feature/onboarding/domain/OnboardingQuizData.kt` — REWRITE

```kotlin
package com.hunterxdk.gymsololeveling.feature.onboarding.domain

import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass

sealed interface QuizOptionType {
    data class ImageAsset(val assetPath: String) : QuizOptionType
    data class Emoji(val emoji: String) : QuizOptionType
}

data class QuizOption(
    val id: String,
    val label: String,
    val type: QuizOptionType,
    val multi: Boolean = false,   // true = checkbox, false = single select
)

data class QuizStep(
    val stepIndex: Int,
    val question: String,
    val subtitle: String = "",
    val options: List<QuizOption>,
    val multiSelect: Boolean = false,  // allow multiple picks
)

object OnboardingQuizData {

    // Gender is collected first to pick male/female physique/bodyfat images
    val STEPS = listOf(

        QuizStep(0, "What's your primary fitness goal?",
            options = listOf(
                QuizOption("build_muscle",     "Build Muscle",     QuizOptionType.ImageAsset("images/onboarding/goals/build_muscle.webp")),
                QuizOption("get_stronger",     "Get Stronger",     QuizOptionType.ImageAsset("images/onboarding/goals/get_stronger.webp")),
                QuizOption("lose_fat",         "Lose Fat",         QuizOptionType.ImageAsset("images/onboarding/goals/lose_fat.webp")),
                QuizOption("improve_health",   "Improve Health",   QuizOptionType.ImageAsset("images/onboarding/goals/improve_health.webp")),
            )
        ),

        QuizStep(1, "How experienced are you?",
            options = listOf(
                QuizOption("beginner",     "Beginner",      QuizOptionType.Emoji("🌱")),
                QuizOption("intermediate", "Intermediate",  QuizOptionType.Emoji("⚡")),
                QuizOption("advanced",     "Advanced",      QuizOptionType.Emoji("🔥")),
                QuizOption("veteran",      "Veteran 5yr+",  QuizOptionType.Emoji("🏆")),
            )
        ),

        QuizStep(2, "How many days per week?",
            options = listOf(
                QuizOption("1_2", "1–2 days", QuizOptionType.Emoji("😴")),
                QuizOption("3_4", "3–4 days", QuizOptionType.Emoji("💪")),
                QuizOption("5_6", "5–6 days", QuizOptionType.Emoji("🔥")),
                QuizOption("7",   "Every day", QuizOptionType.Emoji("🏆")),
            )
        ),

        QuizStep(3, "Your training style?",
            options = listOf(
                QuizOption("powerlifting",  "Powerlifting",  QuizOptionType.Emoji("🏋️")),
                QuizOption("hypertrophy",   "Bodybuilding",  QuizOptionType.Emoji("💪")),
                QuizOption("hiit",          "HIIT / Cardio", QuizOptionType.Emoji("⚡")),
                QuizOption("functional",    "Functional",    QuizOptionType.Emoji("🎯")),
            )
        ),

        QuizStep(4, "What equipment do you have?",
            options = listOf(
                QuizOption("full_gym",       "Full Gym",       QuizOptionType.ImageAsset("images/onboarding/equipment/gym_machines.webp")),
                QuizOption("barbell_plates", "Barbell & Rack", QuizOptionType.ImageAsset("images/onboarding/equipment/barbell_plates.webp")),
                QuizOption("dumbbells",      "Dumbbells",      QuizOptionType.ImageAsset("images/onboarding/equipment/dumbbells.webp")),
                QuizOption("bodyweight",     "Bodyweight Only",QuizOptionType.ImageAsset("images/onboarding/equipment/bodyweight_only.webp")),
            )
        ),

        QuizStep(5, "Your goal physique?", subtitle = "We'll personalise your plan around this",
            // Male images by default; swap to female variants based on prior gender answer if collected
            options = listOf(
                QuizOption("lean_toned",          "Lean & Toned",       QuizOptionType.ImageAsset("images/onboarding/physiques/lean_toned_male.webp")),
                QuizOption("muscular_defined",    "Muscular & Defined", QuizOptionType.ImageAsset("images/onboarding/physiques/muscular_defined_male.webp")),
                QuizOption("balanced_functional", "Balanced",           QuizOptionType.ImageAsset("images/onboarding/physiques/balanced_functional_male.webp")),
                QuizOption("strong_powerful",     "Strong & Powerful",  QuizOptionType.ImageAsset("images/onboarding/physiques/strong_powerful_male.webp")),
            )
        ),

        QuizStep(6, "Any injuries or limitations?", multiSelect = true,
            options = listOf(
                QuizOption("none",        "None",          QuizOptionType.ImageAsset("images/onboarding/injuries/none.webp")),
                QuizOption("lower_back",  "Lower Back",    QuizOptionType.ImageAsset("images/onboarding/injuries/lower_back.webp")),
                QuizOption("knee",        "Knee",          QuizOptionType.ImageAsset("images/onboarding/injuries/knee.webp")),
                QuizOption("shoulder",    "Shoulder",      QuizOptionType.ImageAsset("images/onboarding/injuries/shoulder.webp")),
                QuizOption("wrist_elbow", "Wrist / Elbow", QuizOptionType.ImageAsset("images/onboarding/injuries/wrist_elbow.webp")),
                QuizOption("hip",         "Hip",           QuizOptionType.ImageAsset("images/onboarding/injuries/hip.webp")),
            )
        ),

        QuizStep(7, "What motivates you?",
            options = listOf(
                QuizOption("visual_results",  "Seeing Results",   QuizOptionType.ImageAsset("images/onboarding/motivation/visual_results.webp")),
                QuizOption("data_driven",     "Tracking Data",    QuizOptionType.ImageAsset("images/onboarding/motivation/data_driven.webp")),
                QuizOption("feeling_strong",  "Feeling Strong",   QuizOptionType.ImageAsset("images/onboarding/motivation/feeling_strong.webp")),
                QuizOption("competitive",     "Competition",      QuizOptionType.ImageAsset("images/onboarding/motivation/competitive.webp")),
            )
        ),
    )
}
```

---

## File: `feature/onboarding/domain/PlayerClassAssigner.kt` — NEW

Map 8 answers to `PlayerClass`. Use a score-based approach:

```kotlin
package com.hunterxdk.gymsololeveling.feature.onboarding.domain

import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass

object PlayerClassAssigner {

    /**
     * answers: Map of stepIndex → selected option id(s) (comma-joined if multi-select)
     */
    fun assign(answers: Map<Int, String>): PlayerClass {
        val goal      = answers[0] ?: ""
        val style     = answers[3] ?: ""
        val physique  = answers[5] ?: ""
        val motive    = answers[7] ?: ""
        val equipment = answers[4] ?: ""

        // Score each class
        val scores = mutableMapOf<PlayerClass, Int>()

        fun add(cls: PlayerClass, pts: Int) { scores[cls] = (scores[cls] ?: 0) + pts }

        // Goal
        when (goal) {
            "build_muscle"   -> { add(PlayerClass.MASS_BUILDER, 3); add(PlayerClass.STRENGTH_SEEKER, 1) }
            "get_stronger"   -> { add(PlayerClass.STRENGTH_SEEKER, 3); add(PlayerClass.MASS_BUILDER, 1) }
            "lose_fat"       -> { add(PlayerClass.ENDURANCE_HUNTER, 3); add(PlayerClass.BALANCE_WARRIOR, 1) }
            "improve_health" -> { add(PlayerClass.RECOVERY_SPECIALIST, 3); add(PlayerClass.BALANCE_WARRIOR, 2) }
        }

        // Training style
        when (style) {
            "powerlifting" -> add(PlayerClass.STRENGTH_SEEKER, 3)
            "hypertrophy"  -> add(PlayerClass.MASS_BUILDER, 3)
            "hiit"         -> add(PlayerClass.ENDURANCE_HUNTER, 3)
            "functional"   -> add(PlayerClass.BALANCE_WARRIOR, 3)
        }

        // Physique goal
        when (physique) {
            "lean_toned"          -> add(PlayerClass.ENDURANCE_HUNTER, 2)
            "muscular_defined"    -> add(PlayerClass.MASS_BUILDER, 2)
            "balanced_functional" -> add(PlayerClass.BALANCE_WARRIOR, 2)
            "strong_powerful"     -> add(PlayerClass.STRENGTH_SEEKER, 2)
        }

        // Motivation
        when (motive) {
            "data_driven"    -> add(PlayerClass.ATHLETE_ELITE, 2)
            "competitive"    -> add(PlayerClass.ATHLETE_ELITE, 3)
            "feeling_strong" -> add(PlayerClass.STRENGTH_SEEKER, 1)
            "visual_results" -> add(PlayerClass.MASS_BUILDER, 1)
        }

        // Equipment
        if (equipment == "bodyweight") add(PlayerClass.ENDURANCE_HUNTER, 1)

        return scores.maxByOrNull { it.value }?.key ?: PlayerClass.BALANCE_WARRIOR
    }
}
```

---

## File: `feature/onboarding/presentation/OnboardingQuizScreen.kt` — REWRITE

Key changes from current 140-line version:
1. HorizontalPager over all 8 steps with `userScrollEnabled = false`
2. Progress indicator (LinearProgressIndicator, steps-based)
3. Option cards: image asset version → full `AsyncImage` + label overlay; emoji version → large emoji in Card
4. Multi-select support (step 6 injuries)
5. "Next" button disabled until option selected; last step → navigate to PlayerClassReveal

```kotlin
@Composable
fun OnboardingQuizScreen(
    onComplete: (playerClass: PlayerClass, answers: Map<Int, String>) -> Unit,
) {
    val pagerState = rememberPagerState { OnboardingQuizData.STEPS.size }
    // answers: stepIndex → selected option id (comma-joined for multi)
    val answers = remember { mutableStateMapOf<Int, String>() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Progress bar
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1) / OnboardingQuizData.STEPS.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = GoldAccent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f),
        ) { page ->
            val step = OnboardingQuizData.STEPS[page]
            QuizStepPage(
                step = step,
                selectedIds = answers[page]?.split(",")?.toSet() ?: emptySet(),
                onSelect = { id ->
                    if (step.multiSelect) {
                        val current = answers[page]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
                        if (id == "none") {
                            answers[page] = "none"
                        } else {
                            current.remove("none")
                            if (!current.add(id)) current.remove(id)
                            answers[page] = current.joinToString(",")
                        }
                    } else {
                        answers[page] = id
                    }
                },
            )
        }

        // Next / Finish button
        val currentPage = pagerState.currentPage
        val hasAnswer = !answers[currentPage].isNullOrBlank()
        Button(
            onClick = {
                scope.launch {
                    if (currentPage < OnboardingQuizData.STEPS.size - 1) {
                        pagerState.animateScrollToPage(currentPage + 1)
                    } else {
                        val playerClass = PlayerClassAssigner.assign(answers)
                        onComplete(playerClass, answers.toMap())
                    }
                }
            },
            enabled = hasAnswer,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, disabledContainerColor = GoldAccent.copy(alpha = 0.3f)),
        ) {
            Text(
                if (currentPage == OnboardingQuizData.STEPS.size - 1) "Reveal My Class" else "Next →",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun QuizStepPage(
    step: QuizStep,
    selectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(step.question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (step.subtitle.isNotBlank()) {
            Text(step.subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(16.dp))

        // Image options: 2-column grid; Emoji options: single column
        val hasImages = step.options.any { it.type is QuizOptionType.ImageAsset }
        if (hasImages) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), ...) {
                items(step.options) { option ->
                    ImageOptionCard(option, selected = option.id in selectedIds, onClick = { onSelect(option.id) })
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(step.options) { option ->
                    EmojiOptionCard(option, selected = option.id in selectedIds, onClick = { onSelect(option.id) })
                }
            }
        }
    }
}

@Composable
private fun ImageOptionCard(option: QuizOption, selected: Boolean, onClick: () -> Unit) {
    val assetPath = (option.type as QuizOptionType.ImageAsset).assetPath
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(0.85f),
        border = if (selected) BorderStroke(2.dp, GoldAccent) else null,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box {
            AsyncImage(
                model = "file:///android_asset/$assetPath",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Dark gradient overlay at bottom
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))
                )
            )
            Text(
                option.label,
                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = GoldAccent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun EmojiOptionCard(option: QuizOption, selected: Boolean, onClick: () -> Unit) {
    val emoji = (option.type as QuizOptionType.Emoji).emoji
    Card(
        onClick = onClick,
        border = if (selected) BorderStroke(2.dp, GoldAccent) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) GoldAccent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Default.Check, null, tint = GoldAccent)
        }
    }
}
```

---

## File: `feature/onboarding/presentation/OnboardingViewModel.kt` — UPDATE

Save answers to DataStore after quiz completion:

```kotlin
fun completeQuiz(playerClass: PlayerClass, answers: Map<Int, String>) {
    viewModelScope.launch {
        prefs.setPlayerClass(playerClass.name)
        // Save equipment selection from step 4
        answers[4]?.let { prefs.setAvailableEquipment(it) }
        // Save injury from step 6
        answers[6]?.let { prefs.setInjuries(it) }
        // Save preferred workout days (step 2 → days/week)
        // (days/week is a count, not actual days, so skip for now)
        prefs.setOnboardingDone()
        _navigateToReveal.value = playerClass
    }
}
```

---

## File: `feature/onboarding/presentation/PlayerClassRevealScreen.kt` — UPDATE

Add class artwork from `assets/images/classes/`:

```kotlin
val classAssetName: String get() = when (this) {
    PlayerClass.STRENGTH_SEEKER      -> "strength_seeker"
    PlayerClass.MASS_BUILDER         -> "mass_builder"
    PlayerClass.ENDURANCE_HUNTER     -> "endurance_hunter"
    PlayerClass.BALANCE_WARRIOR      -> "balance_warrior"
    PlayerClass.RECOVERY_SPECIALIST  -> "recovery_specialist"
    PlayerClass.ATHLETE_ELITE        -> "athlete_elite"
}

// In PlayerClassRevealScreen:
AsyncImage(
    model = "file:///android_asset/images/classes/${playerClass.classAssetName}.webp",
    contentDescription = null,
    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(24.dp)),
)
```

Add entrance animation — scale + alpha in with `animateFloatAsState` on initial composition.

---

## NavGraph Update

```kotlin
composable<Screen.OnboardingQuiz> {
    OnboardingQuizScreen(
        onComplete = { playerClass, answers ->
            viewModel.completeQuiz(playerClass, answers)
            navController.navigate(Screen.PlayerClassReveal(playerClass.name)) {
                popUpTo(Screen.OnboardingQuiz) { inclusive = true }
            }
        }
    )
}
```

Add `playerClassName: String` param to `Screen.PlayerClassReveal`:
```kotlin
@Serializable data class PlayerClassReveal(val playerClassName: String) : Screen
```

---

## Execution Order

1. Rewrite `OnboardingQuizData.kt` (sealed QuizOptionType, 8 steps)
2. Create `PlayerClassAssigner.kt`
3. Rewrite `OnboardingQuizScreen.kt` (HorizontalPager, image/emoji cards, multi-select)
4. Update `PlayerClassRevealScreen.kt` (class artwork + animation)
5. Update `OnboardingViewModel.kt` (save answers to DataStore)
6. Update `Screen.PlayerClassReveal` to carry `playerClassName`
7. Wire NavGraph
8. Build + verify

## Verification

1. Fresh install → onboarding → 8 pages render → image cards show APK artwork
2. Select injury "shoulder" + "knee" → both show checkmarks
3. Select "none" → clears other injury selections
4. Complete all 8 steps → PlayerClassReveal shows class artwork
5. Back to home → profile shows correct class name
