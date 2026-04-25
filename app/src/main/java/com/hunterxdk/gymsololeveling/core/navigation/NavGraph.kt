package com.hunterxdk.gymsololeveling.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hunterxdk.gymsololeveling.core.domain.model.UserSession
import com.hunterxdk.gymsololeveling.feature.auth.ui.SignUpScreen
import com.hunterxdk.gymsololeveling.feature.onboarding.presentation.OnboardingQuizScreen
import com.hunterxdk.gymsololeveling.feature.onboarding.presentation.PlayerClassRevealScreen
import com.hunterxdk.gymsololeveling.feature.challenges.presentation.ChallengesScreen
import com.hunterxdk.gymsololeveling.feature.exercise.presentation.CustomExerciseFormScreen
import com.hunterxdk.gymsololeveling.feature.exercise.presentation.CustomExercisesListScreen
import com.hunterxdk.gymsololeveling.feature.exercise.presentation.ExercisePickerScreen
import com.hunterxdk.gymsololeveling.feature.exercise.presentation.YoutubePickerScreen
import com.hunterxdk.gymsololeveling.feature.home.presentation.HomeScreen
import com.hunterxdk.gymsololeveling.feature.history.presentation.WorkoutDetailScreen
import com.hunterxdk.gymsololeveling.feature.history.presentation.WorkoutHistoryScreen
import com.hunterxdk.gymsololeveling.feature.rankings.presentation.MuscleRankingsScreen
import com.hunterxdk.gymsololeveling.feature.profile.presentation.EditProfileScreen
import com.hunterxdk.gymsololeveling.feature.profile.presentation.ProfileScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.AchievementsScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.CustomExercisesScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.EquipmentSettingsScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.InjurySettingsScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.PlateCalculatorScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.PriorityMusclesScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.SettingsScreen
import com.hunterxdk.gymsololeveling.feature.settings.presentation.TrainingScheduleScreen
import com.hunterxdk.gymsololeveling.feature.today.presentation.TodaysWorkoutPreviewScreen
import com.hunterxdk.gymsololeveling.feature.templates.presentation.SavedWorkoutsScreen
import com.hunterxdk.gymsololeveling.feature.templates.presentation.TemplateBuilderScreen
import com.hunterxdk.gymsololeveling.feature.report.presentation.WeeklyReportScreen
import com.hunterxdk.gymsololeveling.feature.streak.presentation.StreakDetailScreen
import com.hunterxdk.gymsololeveling.feature.weight.presentation.WeightTrackerScreen
import com.hunterxdk.gymsololeveling.feature.workout.presentation.ActiveWorkoutScreen
import com.hunterxdk.gymsololeveling.feature.workout.presentation.WorkoutCompleteScreen

@Composable
fun GymLevelsNavGraph(
    modifier: Modifier = Modifier,
    viewModel: NavViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val session by viewModel.session.collectAsState()

    MainScaffold(
        navController = navController,
        content = { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = modifier.padding(padding)
            ) {
                composable<Screen.SignUp> { backStackEntry ->
                    val isMigration = backStackEntry.arguments?.getBoolean("isMigration") ?: false
                    SignUpScreen(
                        onNavigateToOnboarding = { navController.navigate(Screen.Onboarding) },
                        onNavigateToHome = { 
                            navController.popBackStack()
                            navController.navigate(Screen.Home) 
                        },
                        isMigration = isMigration
                    )
                }
                
                composable<Screen.Onboarding> {
                    OnboardingQuizScreen(
                        onComplete = { playerClass, _ ->
                            navController.navigate(Screen.PlayerClassReveal(playerClass.name))
                        }
                    )
                }
                
                composable<Screen.PlayerClassReveal> {
                    val playerClassName = it.arguments?.getString("playerClassName") ?: ""
                    PlayerClassRevealScreen(
                        playerClassName = playerClassName,
                        onContinue = { 
                            navController.popBackStack()
                            navController.navigate(Screen.Home) 
                        }
                    )
                }
                
                composable<Screen.Home> {
                    HomeScreen(
                        onStartWorkout = { navController.navigate(Screen.ActiveWorkout) },
                        onMuscleRankings = { navController.navigate(Screen.MuscleRankings) },
                        onChallenges = { navController.navigate(Screen.Challenges) },
                        onProfile = { navController.navigate(Screen.Profile) }
                    )
                }
                
                composable<Screen.ActiveWorkout> {
                    ActiveWorkoutScreen(
                        onFinishWorkout = { session ->
                            navController.popBackStack()
                            navController.navigate(Screen.WorkoutComplete)
                        },
                        onAddExercise = { sessionId ->
                            navController.navigate(Screen.ExercisePicker(sessionId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.WorkoutDetail> { backStackEntry ->
                    val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
                    WorkoutDetailScreen(
                        workoutId = workoutId,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.WorkoutHistory> {
                    WorkoutHistoryScreen(
                        onSelectWorkout = { workoutId ->
                            navController.navigate(Screen.WorkoutDetail(workoutId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.WorkoutComplete> {
                    WorkoutCompleteScreen(
                        onHome = { 
                            navController.popBackStack()
                            navController.navigate(Screen.Home) 
                        },
                        onViewHistory = { 
                            navController.popBackStack()
                            navController.navigate(Screen.WorkoutHistory) 
                        }
                    )
                }
                
                composable<Screen.TodaysWorkoutPreview> {
                    TodaysWorkoutPreviewScreen(
                        onStartWorkout = { navController.navigate(Screen.ActiveWorkout) },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.SavedWorkouts> {
                    SavedWorkoutsScreen(
                        onSelectWorkout = { navController.popBackStack(); navController.navigate(Screen.ActiveWorkout) },
                        onCreateTemplate = { navController.navigate(Screen.TemplateBuilder) },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.TemplateBuilder> {
                    TemplateBuilderScreen(
                        onSave = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.ExercisePicker> { backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                    ExercisePickerScreen(
                        onSelect = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.MuscleRankings> {
                    MuscleRankingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.Profile> {
                    ProfileScreen(
                        onEditProfile = { navController.navigate(Screen.EditProfile) },
                        onAchievements = { navController.navigate(Screen.Achievements) },
                        onChallenges = { navController.navigate(Screen.Challenges) },
                        onStreak = { navController.navigate(Screen.StreakDetail) },
                        onWeightTracker = { navController.navigate(Screen.WeightTracker) },
                        onWeeklyReport = { navController.navigate(Screen.WeeklyReport) },
                        onSettings = { navController.navigate(Screen.Settings) },
                        onSignOut = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.EditProfile> {
                    EditProfileScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.Achievements> {
                    AchievementsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.Challenges> {
                    ChallengesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.StreakDetail> {
                    StreakDetailScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.WeightTracker> {
                    WeightTrackerScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.WeeklyReport> {
                    WeeklyReportScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.Settings> {
                    SettingsScreen(
                        onCustomExercises = { navController.navigate(Screen.CustomExercises) },
                        onTrainingSchedule = { navController.navigate(Screen.TrainingSchedule) },
                        onEquipmentSettings = { navController.navigate(Screen.EquipmentSettings) },
                        onInjurySettings = { navController.navigate(Screen.InjurySettings) },
                        onPriorityMuscles = { navController.navigate(Screen.PriorityMuscles) },
                        onPlateCalculator = { navController.navigate(Screen.PlateCalculator) },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.CustomExercises> {
                    CustomExercisesListScreen(
                        onAddExercise = { navController.navigate(Screen.CustomExerciseForm) },
                        onEditExercise = { exerciseId ->
                            navController.navigate(Screen.CustomExerciseForm(exerciseId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.CustomExerciseForm> { backStackEntry ->
                    val exerciseId = backStackEntry.arguments?.getString("exerciseId")
                    CustomExerciseFormScreen(
                        exerciseId = exerciseId,
                        onSave = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.YoutubePicker> { backStackEntry ->
                    val currentVideoId = backStackEntry.arguments?.getString("currentVideoId")
                    YoutubePickerScreen(
                        currentVideoId = currentVideoId,
                        onSelect = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.TrainingSchedule> {
                    TrainingScheduleScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.EquipmentSettings> {
                    EquipmentSettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.InjurySettings> {
                    InjurySettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.PriorityMuscles> {
                    PriorityMusclesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable<Screen.PlateCalculator> {
                    PlateCalculatorScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        },
        showBottomNav = session is UserSession.Authenticated || session is UserSession.Guest
    )
}
