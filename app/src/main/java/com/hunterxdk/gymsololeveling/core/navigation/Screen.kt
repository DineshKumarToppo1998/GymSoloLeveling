package com.hunterxdk.gymsololeveling.core.navigation

import kotlinx.serialization.Serializable

object NavConstants {
    const val BottomNavRoute = "main_bottom_nav"
}

sealed interface Screen {
    val route: String

    @Serializable object Splash : Screen {
        override val route: String = "splash"
    }
    @Serializable object Onboarding : Screen {
        override val route: String = "onboarding"
    }
    @Serializable object OnboardingQuiz : Screen {
        override val route: String = "onboardingQuiz"
    }
    @Serializable data class PlayerClassReveal(val playerClassName: String = "") : Screen {
        override val route: String = "playerClassReveal"
    }
    @Serializable data class SignUp(val isMigration: Boolean = false) : Screen {
        override val route: String = "signUp"
    }
    @Serializable object Home : Screen {
        override val route: String = "home"
    }
    @Serializable object ActiveWorkout : Screen {
        override val route: String = "activeWorkout"
    }
    @Serializable data class WorkoutDetail(val workoutId: String) : Screen {
        override val route: String = "workoutDetail/{workoutId}"
    }
    @Serializable object WorkoutHistory : Screen {
        override val route: String = "workoutHistory"
    }
    @Serializable object WorkoutComplete : Screen {
        override val route: String = "workoutComplete"
    }
    @Serializable object TodaysWorkoutPreview : Screen {
        override val route: String = "todaysWorkoutPreview"
    }
    @Serializable object SavedWorkouts : Screen {
        override val route: String = "savedWorkouts"
    }
    @Serializable object TemplateBuilder : Screen {
        override val route: String = "templateBuilder"
    }
    @Serializable data class ExercisePicker(val sessionId: String) : Screen {
        override val route: String = "exercisePicker/{sessionId}"
    }
    @Serializable object MuscleRankings : Screen {
        override val route: String = "muscleRankings"
    }
    @Serializable object Profile : Screen {
        override val route: String = "profile"
    }
    @Serializable object EditProfile : Screen {
        override val route: String = "editProfile"
    }
    @Serializable object Achievements : Screen {
        override val route: String = "achievements"
    }
    @Serializable object Challenges : Screen {
        override val route: String = "challenges"
    }
    @Serializable object StreakDetail : Screen {
        override val route: String = "streakDetail"
    }
    @Serializable object WeightTracker : Screen {
        override val route: String = "weightTracker"
    }
    @Serializable object WeeklyReport : Screen {
        override val route: String = "weeklyReport"
    }
    @Serializable object Settings : Screen {
        override val route: String = "settings"
    }
    @Serializable object CustomExercises : Screen {
        override val route: String = "customExercises"
    }
    @Serializable data class CustomExerciseForm(val exerciseId: String? = null) : Screen {
        override val route: String = "customExerciseForm/{exerciseId}"
    }
    @Serializable data class YoutubePicker(val currentVideoId: String? = null) : Screen {
        override val route: String = "youtubePicker"
    }
    @Serializable object TrainingSchedule : Screen {
        override val route: String = "trainingSchedule"
    }
    @Serializable object EquipmentSettings : Screen {
        override val route: String = "equipmentSettings"
    }
    @Serializable object InjurySettings : Screen {
        override val route: String = "injurySettings"
    }
    @Serializable object PriorityMuscles : Screen {
        override val route: String = "priorityMuscles"
    }
    @Serializable object PlateCalculator : Screen {
        override val route: String = "plateCalculator"
    }
}
