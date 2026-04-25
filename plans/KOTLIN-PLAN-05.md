# KOTLIN-PLAN-05.md — Onboarding Quiz & Player Class Assignment

## Goal
Implement the 8-step onboarding quiz using HorizontalPager, PlayerClassAssigner logic that maps quiz answers to a PlayerClass, and the PlayerClassRevealScreen with animation.

## Phase
Foundation — Phase 3 of 6. Depends on KOTLIN-PLAN-03, KOTLIN-PLAN-04.

---

## Screens
1. `OnboardingQuizScreen` — 8-step horizontal pager quiz
2. `PlayerClassRevealScreen` — animated reveal of assigned class

---

## Files to Create

### `feature/onboarding/domain/OnboardingQuizStep.kt`
```kotlin
package com.example.gymlevels.feature.onboarding.domain

data class QuizStep(
    val stepIndex: Int,
    val question: String,
    val options: List<QuizOption>,
)

data class QuizOption(
    val id: String,
    val label: String,
    val iconEmoji: String = "",
)

object OnboardingQuizData {
    val STEPS = listOf(
        QuizStep(0, "What's your primary fitness goal?", listOf(
            QuizOption("build_muscle", "Build Muscle", "💪"),
            QuizOption("get_stronger", "Get Stronger", "🏋️"),
            QuizOption("lose_fat", "Lose Fat", "🔥"),
            QuizOption("improve_health", "Improve Health", "❤️"),
            QuizOption("improve_endurance", "Improve Endurance", "🏃"),
            QuizOption("increase_flexibility", "Increase Flexibility", "🧘"),
        )),
        QuizStep(1, "How experienced are you with training?", listOf(
            QuizOption("beginner", "Beginner (< 6 months)", "🌱"),
            QuizOption("intermediate", "Intermediate (6m–2yr)", "⚡"),
            QuizOption("advanced", "Advanced (2–5yr)", "🔥"),
            QuizOption("veteran", "Veteran (5+ years)", "🏆"),
        )),
        QuizStep(2, "How many days per week do you train?", listOf(
            QuizOption("1_2", "1–2 days", "😴"),
            QuizOption("3_4", "3–4 days", "💪"),
            QuizOption("5_6", "5–6 days", "🔥"),
            QuizOption("7", "Every day", "🏆"),
        )),
        QuizStep(3, "Which training style appeals to you most?", listOf(
            QuizOption("powerlifting", "Powerlifting / Heavy lifts", "🏋️"),
            QuizOption("hypertrophy", "Bodybuilding / Hypertrophy", "💪"),
            QuizOption("hiit", "HIIT / Cardio circuits", "⚡"),
            QuizOption("functional", "Functional / Athletic", "🎯"),
        )),
        QuizStep(4, "What equipment do you have access to?", listOf(
            QuizOption("full_gym", "Full gym", "🏢"),
            QuizOption("dumbbells", "Dumbbells + Barbell", "🏋️"),
            QuizOption("dumbbells_only", "Dumbbells only", "💪"),
            QuizOption("bodyweight", "Bodyweight only", "🤸"),
        )),
        QuizStep(5, "How long are your typical workouts?", listOf(
            QuizOption("under_30", "Under 30 min", "⚡"),
            QuizOption("30_45", "30–45 min", "⏱️"),
            QuizOption("45_60", "45–60 min", "💪"),
            QuizOption("over_60", "Over 60 min", "🔥"),
        )),
        QuizStep(6, "How would you describe your recovery?", listOf(
            QuizOption("fast", "I recover fast, train often", "⚡"),
            QuizOption("normal", "Normal recovery, 48h rest", "✅"),
            QuizOption("slow", "I need more rest days", "😴"),
            QuizOption("focus_recovery", "Recovery is a priority for me", "🧘"),
        )),
        QuizStep(7, "What motivates you most?", listOf(
            QuizOption("personal_records", "Breaking personal records", "🏆"),
            QuizOption("aesthetics", "Looking and feeling great", "💎"),
            QuizOption("performance", "Athletic performance", "⚡"),
            QuizOption("longevity", "Health and longevity", "❤️"),
        )),
    )
}
```

### `feature/onboarding/domain/PlayerClassAssigner.kt`
```kotlin
package com.example.gymlevels.feature.onboarding.domain

object PlayerClassAssigner {
    fun assign(answers: Map<Int, String>): PlayerClass {
        var strengthScore = 0
        var massScore = 0
        var enduranceScore = 0
        var recoveryScore = 0
        var athleteScore = 0
        var balanceScore = 0

        // Goal mapping
        when (answers[0]) {
            "get_stronger" -> strengthScore += 3
            "build_muscle" -> massScore += 3
            "improve_endurance" -> enduranceScore += 3
            "lose_fat" -> { enduranceScore += 1; balanceScore += 1 }
            "improve_health" -> { balanceScore += 2; recoveryScore += 1 }
            "increase_flexibility" -> recoveryScore += 3
        }

        // Training style
        when (answers[3]) {
            "powerlifting" -> strengthScore += 3
            "hypertrophy" -> massScore += 3
            "hiit" -> { enduranceScore += 2; athleteScore += 1 }
            "functional" -> athleteScore += 3
        }

        // Frequency
        when (answers[2]) {
            "7" -> { strengthScore += 1; massScore += 1; athleteScore += 1 }
            "5_6" -> { massScore += 1; enduranceScore += 1 }
            "3_4" -> balanceScore += 1
            "1_2" -> recoveryScore += 1
        }

        // Recovery
        when (answers[6]) {
            "fast" -> { strengthScore += 1; athleteScore += 1 }
            "focus_recovery" -> recoveryScore += 3
            "slow" -> recoveryScore += 2
        }

        // Motivation
        when (answers[7]) {
            "personal_records" -> strengthScore += 2
            "aesthetics" -> massScore += 2
            "performance" -> athleteScore += 2
            "longevity" -> recoveryScore += 2
        }

        val scores = mapOf(
            PlayerClass.STRENGTH_SEEKER to strengthScore,
            PlayerClass.MASS_BUILDER to massScore,
            PlayerClass.ENDURANCE_HUNTER to enduranceScore,
            PlayerClass.RECOVERY_SPECIALIST to recoveryScore,
            PlayerClass.ATHLETE_ELITE to athleteScore,
            PlayerClass.BALANCE_WARRIOR to balanceScore,
        )

        return scores.maxByOrNull { it.value }?.key ?: PlayerClass.BALANCE_WARRIOR
    }
}
```

### `feature/onboarding/presentation/OnboardingViewModel.kt`
```kotlin
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefsDataStore: UserPreferencesDataStore,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _answers = mutableStateMapOf<Int, String>()
    val answers: Map<Int, String> get() = _answers

    private val _assignedClass = MutableStateFlow<PlayerClass?>(null)
    val assignedClass: StateFlow<PlayerClass?> = _assignedClass.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    fun answerStep(stepIndex: Int, optionId: String) {
        _answers[stepIndex] = optionId
    }

    fun finishQuiz() {
        val playerClass = PlayerClassAssigner.assign(_answers)
        _assignedClass.value = playerClass

        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            // Persist to Firestore
            firestore.collection("users").document(uid).update(mapOf(
                "playerClass" to playerClass.name,
                "hasCompletedOnboarding" to true,
                "availableEquipment" to resolveEquipment(_answers[4]),
            )).await()
            prefsDataStore.setOnboardingDone()
        }
    }

    fun onRevealComplete() { _isComplete.value = true }

    private fun resolveEquipment(equipmentAnswer: String?): List<String> = when (equipmentAnswer) {
        "full_gym" -> listOf("barbell", "dumbbell", "cable", "machine", "smith_machine", "rack", "bench")
        "dumbbells" -> listOf("dumbbell", "barbell", "bench")
        "dumbbells_only" -> listOf("dumbbell")
        "bodyweight" -> listOf("bodyweight")
        else -> listOf("bodyweight")
    }
}
```

### `feature/onboarding/presentation/OnboardingQuizScreen.kt`
```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingQuizScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState { OnboardingQuizData.STEPS.size }
    val answers by rememberUpdatedState(viewModel.answers)
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / OnboardingQuizData.STEPS.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            "${pagerState.currentPage + 1} / ${OnboardingQuizData.STEPS.size}",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f)
        ) { page ->
            val step = OnboardingQuizData.STEPS[page]
            QuizStepPage(
                step = step,
                selectedOption = answers[page],
                onOptionSelected = { optionId ->
                    viewModel.answerStep(page, optionId)
                    scope.launch {
                        if (page < OnboardingQuizData.STEPS.size - 1) {
                            delay(300)
                            pagerState.animateScrollToPage(page + 1)
                        } else {
                            viewModel.finishQuiz()
                            onComplete()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun QuizStepPage(
    step: QuizStep,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(step.question, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))

        step.options.forEach { option ->
            val isSelected = selectedOption == option.id
            Card(
                onClick = { onOptionSelected(option.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(option.iconEmoji, style = MaterialTheme.typography.titleLarge)
                    Text(option.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
```

### `feature/onboarding/presentation/PlayerClassRevealScreen.kt`
```kotlin
@Composable
fun PlayerClassRevealScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val assignedClass by viewModel.assignedClass.collectAsStateWithLifecycle()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); visible = true }

    val alpha by animateFloatAsState(if (visible) 1f else 0f, animationSpec = tween(800))
    val scale by animateFloatAsState(if (visible) 1f else 0.7f, animationSpec = spring(dampingRatio = 0.6f, stiffnessRatio = Spring.StiffnessMedium))

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your Class", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        Spacer(Modifier.height(24.dp))

        assignedClass?.let { cls ->
            Box(
                Modifier
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Player class image
                    Image(
                        painter = painterResource(cls.toImageRes()),
                        contentDescription = cls.displayName,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(cls.displayName, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text(cls.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { viewModel.onRevealComplete(); onContinue() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Start Your Journey", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun PlayerClass.toImageRes(): Int = when (this) {
    PlayerClass.STRENGTH_SEEKER -> R.drawable.class_strength_seeker
    PlayerClass.MASS_BUILDER -> R.drawable.class_mass_builder
    PlayerClass.ENDURANCE_HUNTER -> R.drawable.class_endurance_hunter
    PlayerClass.RECOVERY_SPECIALIST -> R.drawable.class_recovery_specialist
    PlayerClass.ATHLETE_ELITE -> R.drawable.class_athlete_elite
    PlayerClass.BALANCE_WARRIOR -> R.drawable.class_balance_warrior
}
```

---

## Verification
1. After sign-up, new user navigates to OnboardingQuizScreen
2. Answering all 8 questions auto-advances pager, last answer triggers finishQuiz()
3. PlayerClassRevealScreen shows correct class with scale+fade animation
4. Firestore `users/{uid}` document has `playerClass` and `hasCompletedOnboarding: true`
5. Returning user (onboarding done) skips quiz entirely → goes directly to Home
