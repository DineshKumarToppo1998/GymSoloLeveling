package com.hunterxdk.gymsololeveling.core.domain.model.enums

enum class BadgeType { MILESTONE, CONSISTENCY, PR_HUNTER, VOLUME, EXPLORER, STRENGTH, MUSCLE_MASTER, SPECIAL }

enum class ChallengeDifficulty { EASY, MEDIUM, HARD }

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
