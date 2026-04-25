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
    val multi: Boolean = false,
)

data class QuizStep(
    val stepIndex: Int,
    val question: String,
    val subtitle: String = "",
    val options: List<QuizOption>,
    val multiSelect: Boolean = false,
)

object OnboardingQuizData {

    val STEPS = listOf(
        QuizStep(0, "What's your primary fitness goal?",
            options = listOf(
                QuizOption("build_muscle",     "Build Muscle",      QuizOptionType.ImageAsset("images/onboarding/goals/build_muscle.webp")),
                QuizOption("get_stronger",     "Get Stronger",      QuizOptionType.ImageAsset("images/onboarding/goals/get_stronger.webp")),
                QuizOption("lose_fat",         "Lose Fat",          QuizOptionType.ImageAsset("images/onboarding/goals/lose_fat.webp")),
                QuizOption("improve_health",   "Improve Health",    QuizOptionType.ImageAsset("images/onboarding/goals/improve_health.webp")),
            )
        ),
        QuizStep(1, "How experienced are you?",
            options = listOf(
                QuizOption("beginner",     "Beginner",      QuizOptionType.Emoji("\uD83C\uDF31")),
                QuizOption("intermediate", "Intermediate",  QuizOptionType.Emoji("\u26A1")),
                QuizOption("advanced",     "Advanced",      QuizOptionType.Emoji("\uD83D\uDD25")),
                QuizOption("veteran",       "Veteran 5yr+",  QuizOptionType.Emoji("\uD83C\uDFC6")),
            )
        ),
        QuizStep(2, "How many days per week?",
            options = listOf(
                QuizOption("1_2", "1–2 days",  QuizOptionType.Emoji("\uD83D\uDE34")),
                QuizOption("3_4", "3–4 days",  QuizOptionType.Emoji("\uD83D\uDCAA")),
                QuizOption("5_6", "5–6 days",  QuizOptionType.Emoji("\uD83D\uDD25")),
                QuizOption("7",   "Every day", QuizOptionType.Emoji("\uD83C\uDFC6")),
            )
        ),
        QuizStep(3, "Your training style?",
            options = listOf(
                QuizOption("powerlifting",  "Powerlifting",  QuizOptionType.Emoji("\uD83C\uDFCB")),
                QuizOption("hypertrophy",   "Bodybuilding",  QuizOptionType.Emoji("\uD83D\uDCAA")),
                QuizOption("hiit",          "HIIT / Cardio", QuizOptionType.Emoji("\u26A1")),
                QuizOption("functional",    "Functional",   QuizOptionType.Emoji("\uD83C\uDFFB")),
            )
        ),
        QuizStep(4, "What equipment do you have?",
            options = listOf(
                QuizOption("full_gym",       "Full Gym",        QuizOptionType.ImageAsset("images/onboarding/equipment/full_gym.webp")),
                QuizOption("barbell_plates", "Barbell & Rack",  QuizOptionType.ImageAsset("images/onboarding/equipment/barbell_plates.webp")),
                QuizOption("dumbbells",      "Dumbbells",       QuizOptionType.ImageAsset("images/onboarding/equipment/dumbbells.webp")),
                QuizOption("bodyweight",     "Bodyweight Only", QuizOptionType.ImageAsset("images/onboarding/equipment/bodyweight_only.webp")),
            )
        ),
        QuizStep(5, "Your goal physique?", subtitle = "We'll personalise your plan around this",
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
                QuizOption("visual_results",  "Seeing Results",  QuizOptionType.ImageAsset("images/onboarding/motivation/visual_results.webp")),
                QuizOption("data_driven",     "Tracking Data",   QuizOptionType.ImageAsset("images/onboarding/motivation/data_driven.webp")),
                QuizOption("feeling_strong",  "Feeling Strong",  QuizOptionType.ImageAsset("images/onboarding/motivation/feeling_strong.webp")),
                QuizOption("competitive",     "Competition",     QuizOptionType.ImageAsset("images/onboarding/motivation/competitive.webp")),
            )
        ),
    )
}