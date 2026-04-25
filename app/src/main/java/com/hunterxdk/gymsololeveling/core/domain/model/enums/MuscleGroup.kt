package com.hunterxdk.gymsololeveling.core.domain.model.enums

enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    FRONT_SHOULDERS("Front Shoulders"),
    BACK_SHOULDERS("Back Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    UPPER_BACK("Upper Back"),
    MIDDLE_BACK("Middle Back"),
    LOWER_BACK("Lower Back"),
    LATS("Lats"),
    TRAPS("Traps"),
    ABS("Abs"),
    OBLIQUES("Obliques"),
    QUADRICEPS("Quadriceps"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
}

fun String.toMuscleGroup(): MuscleGroup? = MuscleGroup.entries.firstOrNull {
    it.name.equals(this.replace(" ", "_"), ignoreCase = true) ||
    it.displayName.equals(this, ignoreCase = true)
}
