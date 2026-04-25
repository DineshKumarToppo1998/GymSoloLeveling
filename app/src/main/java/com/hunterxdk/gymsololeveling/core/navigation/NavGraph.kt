package com.hunterxdk.gymsololeveling.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hunterxdk.gymsololeveling.core.domain.model.UserSession
import com.hunterxdk.gymsololeveling.feature.achievements.presentation.AchievementsScreen
import com.hunterxdk.gymsololeveling.feature.auth.ui.SignUpScreen
import com.hunterxdk.gymsololeveling.feature.calculator.presentation.PlateCalculatorScreen
import com.hunterxdk.gymsololeveling.feature.challenges.presentation.ChallengesScreen
import com.hunterxdk.gymsololeveling.feature.exercise.presentation.CustomExerciseFormScreen
import com.hunterxdk.gymsololeveling.feature.exercise.presentation.CustomExercisesListScreen
import com.hunterxdk.gymsololeveling.feature.exercise.presentation.ExercisePickerScreen
import com.hunterxdk.gymsololeveling.feature.exercise.presentation.YoutubePickerScreen
import com.hunterxdk.gymsololeveling.feature.history.presentation.WorkoutDetailScreen
import com.hunterxdk.gymsololeveling.feature.history.presentation.WorkoutHistoryScreen
import com.hunterxdk.gymsololeveling.feature.home.presentation.HomeScreen
import com.hunterxdk.gymsololeveling.feature.onboarding.presentation.OnboardingQuizScreen
import com.hunterxdk.gymsololeveling.feature.onboarding.presentation.OnboardingViewModel
import com.hunterxdk.gymsololeveling.feature.onboarding.presentation.PlayerClassRevealScreen
import com.hunterxdk.gymsololeveling.feature.profile.presentation.EditProfileScreen
import com.hunterxdk.gymsololeveling.feature.profile.presentation.ProfileScreen
import com.hunterxdk.gymsololeveling.feature.rankings.presentation.MuscleRankingsScreen
import com.hunterxdk.gymsololeveling.feature.report.presentation.WeeklyReportScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.EquipmentSettingsScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.InjurySettingsScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.PriorityMusclesScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.SettingsScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.TrainingScheduleScreen
import com.hunterxdk.gymsololeveling.feature.streak.presentation.StreakDetailScreen
import com.hunterxdk.gymsololeveling.feature.templates.presentation.SavedWorkoutsScreen
import com.hunterxdk.gymsololeveling.feature.templates.presentation.TemplateBuilderScreen
import com.hunterxdk.gymsololeveling.feature.today.presentation.TodaysWorkoutPreviewScreen
import com.hunterxdk.gymsololeveling.feature.weight.presentation.WeightTrackerScreen
import com.hunterxdk.gymsololeveling.feature.workout.presentation.ActiveWorkoutScreen
import com.hunterxdk.gymsololeveling.feature.workout.presentation.ActiveWorkoutViewModel
import com.hunterxdk.gymsololeveling.feature.workout.presentation.WorkoutCompleteScreen
import kotlinx.coroutines.delay

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun GymLevelsNavGraph(
    modifier: Modifier = Modifier,
    viewModel: NavViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val session by viewModel.session.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // Bottom nav only on the 4 main tab destinations
    val showBottomNav = navBackStackEntry?.destination?.route?.let { route ->
        BottomNavItem.entries.any { item -> route == item.screen::class.qualifiedName }
    } == true

    // Initial navigation: from Splash to Home (returning users) when session resolves.
    // Only fires navigation if we are still on the Splash screen, so it does not
    // interfere with navigation triggered by SignUpScreen for new sign-ins or guests.
    LaunchedEffect(session) {
        val onSplash = navController.currentBackStackEntry?.destination?.route
            ?.contains("Splash", ignoreCase = true) == true
        when (session) {
            is UserSession.Authenticated -> {
                if (onSplash) navController.navigate(Screen.Home) { popUpTo(0) { inclusive = true } }
            }
            is UserSession.Guest -> {
                if (onSplash) navController.navigate(Screen.Home) { popUpTo(0) { inclusive = true } }
            }
            is UserSession.Loading -> {}
        }
    }

    // Fallback: new users have no Firebase auth and no guest session, so SessionManager
    // leaves session as Loading indefinitely. After 800ms still on Splash → show SignUp.
    LaunchedEffect(Unit) {
        delay(800L)
        val onSplash = navController.currentBackStackEntry?.destination?.route
            ?.contains("Splash", ignoreCase = true) == true
        if (onSplash) {
            navController.navigate(Screen.SignUp()) { popUpTo(0) { inclusive = true } }
        }
    }

    MainScaffold(
        navController = navController,
        showBottomNav = showBottomNav,
        content = { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash,
                modifier = modifier.padding(padding),
            ) {

                // ── Splash ────────────────────────────────────────────────────────────
                composable<Screen.Splash> { SplashScreen() }

                // ── Auth & Onboarding (no bottom nav) ─────────────────────────────────
                composable<Screen.SignUp> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.SignUp>()
                    SignUpScreen(
                        isMigration = args.isMigration,
                        onNavigateToOnboarding = {
                            navController.navigate(Screen.OnboardingQuiz) {
                                popUpTo(Screen.SignUp(args.isMigration)) { inclusive = true }
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }

                composable<Screen.OnboardingQuiz> { backStackEntry ->
                    val onboardingVm: OnboardingViewModel = hiltViewModel(backStackEntry)
                    OnboardingQuizScreen(
                        onComplete = { playerClass, answers ->
                            onboardingVm.completeQuiz(playerClass, answers)
                            navController.navigate(Screen.PlayerClassReveal(playerClass.name)) {
                                popUpTo(Screen.OnboardingQuiz) { inclusive = true }
                            }
                        },
                        viewModel = onboardingVm,
                    )
                }

                composable<Screen.PlayerClassReveal> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.PlayerClassReveal>()
                    PlayerClassRevealScreen(
                        playerClassName = args.playerClassName,
                        onContinue = {
                            navController.navigate(Screen.Home) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }

                // ── Main tabs (bottom nav visible) ────────────────────────────────────
                composable<Screen.Home> {
                    HomeScreen(
                        onStartWorkout = { navController.navigate(Screen.ActiveWorkout) },
                        onMuscleRankings = { navController.navigate(Screen.MuscleRankings) },
                        onChallenges = { navController.navigate(Screen.Challenges) },
                        onProfile = { navController.navigate(Screen.Profile) },
                    )
                }

                composable<Screen.WorkoutHistory> {
                    WorkoutHistoryScreen(
                        onWorkoutSelected = { workoutId ->
                            navController.navigate(Screen.WorkoutDetail(workoutId))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.MuscleRankings> {
                    MuscleRankingsScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.Profile> {
                    ProfileScreen(
                        onSettings = { navController.navigate(Screen.Settings) },
                        onSignOut = {
                            navController.navigate(Screen.SignUp()) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onAchievements = { navController.navigate(Screen.Achievements) },
                        onWeightTracker = { navController.navigate(Screen.WeightTracker) },
                        onBack = { navController.popBackStack() },
                        onSignUp = { navController.navigate(Screen.SignUp(isMigration = true)) },
                    )
                }

                // ── Workout flow ──────────────────────────────────────────────────────
                composable<Screen.ActiveWorkout> { backStackEntry ->
                    val activeVm: ActiveWorkoutViewModel = hiltViewModel(backStackEntry)
                    ActiveWorkoutScreen(
                        onFinishWorkout = { _ ->
                            // ActiveWorkout stays in back-stack so WorkoutComplete can
                            // retrieve its ViewModel and access the completed session.
                            navController.navigate(Screen.WorkoutComplete)
                        },
                        onAddExercise = { sessionId ->
                            navController.navigate(Screen.ExercisePicker(sessionId))
                        },
                        onBack = { navController.popBackStack() },
                        viewModel = activeVm,
                    )
                }

                composable<Screen.ExercisePicker> { backStackEntry ->
                    val activeWorkoutEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<Screen.ActiveWorkout>()
                    }
                    val activeVm: ActiveWorkoutViewModel = hiltViewModel(activeWorkoutEntry)
                    ExercisePickerScreen(
                        onExerciseSelected = { exercise ->
                            activeVm.addExercise(exercise)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.WorkoutComplete> { backStackEntry ->
                    val activeWorkoutEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<Screen.ActiveWorkout>()
                    }
                    val activeVm: ActiveWorkoutViewModel = hiltViewModel(activeWorkoutEntry)
                    val workoutSession by activeVm.session.collectAsState()
                    workoutSession?.let { s ->
                        WorkoutCompleteScreen(
                            session = s,
                            onDone = {
                                navController.navigate(Screen.Home) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }

                composable<Screen.WorkoutDetail> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.WorkoutDetail>()
                    WorkoutDetailScreen(
                        workoutId = args.workoutId,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.TodaysWorkoutPreview> {
                    TodaysWorkoutPreviewScreen(
                        onStartWorkout = { navController.navigate(Screen.ActiveWorkout) },
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Templates ─────────────────────────────────────────────────────────
                composable<Screen.SavedWorkouts> {
                    SavedWorkoutsScreen(
                        onCreateTemplate = { navController.navigate(Screen.TemplateBuilder) },
                        onTemplateSelected = { navController.navigate(Screen.TemplateBuilder) },
                        onStartFromTemplate = { navController.navigate(Screen.ActiveWorkout) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.TemplateBuilder> {
                    TemplateBuilderScreen(
                        templateId = null,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Profile sub-screens ───────────────────────────────────────────────
                composable<Screen.EditProfile> {
                    EditProfileScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.Achievements> {
                    AchievementsScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.StreakDetail> {
                    StreakDetailScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.WeightTracker> {
                    WeightTrackerScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.WeeklyReport> {
                    WeeklyReportScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Challenges ────────────────────────────────────────────────────────
                composable<Screen.Challenges> {
                    ChallengesScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Settings ──────────────────────────────────────────────────────────
                composable<Screen.Settings> {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onSignedOut = {
                            navController.navigate(Screen.SignUp()) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onEquipment = { navController.navigate(Screen.EquipmentSettings) },
                        onInjuries = { navController.navigate(Screen.InjurySettings) },
                        onPriorityMuscles = { navController.navigate(Screen.PriorityMuscles) },
                        onTrainingSchedule = { navController.navigate(Screen.TrainingSchedule) },
                    )
                }

                composable<Screen.TrainingSchedule> {
                    TrainingScheduleScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.EquipmentSettings> {
                    EquipmentSettingsScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.InjurySettings> {
                    InjurySettingsScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.PriorityMuscles> {
                    PriorityMusclesScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Custom exercises ──────────────────────────────────────────────────
                composable<Screen.CustomExercises> {
                    CustomExercisesListScreen(
                        onCreateExercise = { navController.navigate(Screen.CustomExerciseForm()) },
                        onEditExercise = { exerciseId ->
                            navController.navigate(Screen.CustomExerciseForm(exerciseId))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.CustomExerciseForm> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.CustomExerciseForm>()
                    CustomExerciseFormScreen(
                        exerciseId = args.exerciseId,
                        onSaved = { navController.popBackStack() },
                        onPickYoutube = { currentVideoId ->
                            navController.navigate(Screen.YoutubePicker(currentVideoId))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.YoutubePicker> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.YoutubePicker>()
                    YoutubePickerScreen(
                        currentVideoId = args.currentVideoId,
                        onVideoSelected = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Utilities ─────────────────────────────────────────────────────────
                composable<Screen.PlateCalculator> {
                    PlateCalculatorScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        },
    )
}
