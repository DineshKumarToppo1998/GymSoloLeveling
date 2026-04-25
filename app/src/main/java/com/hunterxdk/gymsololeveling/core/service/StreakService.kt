package com.hunterxdk.gymsololeveling.core.service

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakService @Inject constructor() {

    data class StreakData(
        val currentStreak: Int,
        val bestStreak: Int,
        val streakStartDate: LocalDate?,
    )

    fun getStreakData(workoutDates: List<Long>): StreakData {
        val result = calculateStreak(workoutDates)
        val startDate = if (result.currentStreak > 0) {
            calculateStreakStartDate(workoutDates, result.currentStreak)
        } else null
        return StreakData(
            currentStreak = result.currentStreak,
            bestStreak = result.longestStreak,
            streakStartDate = startDate,
        )
    }

    private fun calculateStreakStartDate(workoutDates: List<Long>, streak: Int): LocalDate? {
        if (streak == 0 || workoutDates.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        val sortedDates = workoutDates
            .map { millis -> LocalDate.ofInstant(Instant.ofEpochMilli(millis), zone) }
            .distinct()
            .sortedDescending()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val expectedDate = if (sortedDates.first() == today) today else yesterday
        var currentDate = expectedDate
        for (i in 0 until streak) {
            currentDate = currentDate.minusDays(1)
        }
        return currentDate.plusDays(1)
    }

    /**
     * Calculate current streak from a list of workout timestamps (epoch millis).
     * A streak is consecutive days where at least one workout was logged.
     * Streak remains active if the last workout was today or yesterday.
     */
    fun calculateStreak(workoutDates: List<Long>): StreakResult {
        if (workoutDates.isEmpty()) return StreakResult(0, 0, null)

        val zone = ZoneId.systemDefault()
        val sortedDates = workoutDates
            .map { millis -> LocalDate.ofInstant(Instant.ofEpochMilli(millis), zone) }
            .distinct()
            .sortedDescending()

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        if (sortedDates.first() != today && sortedDates.first() != yesterday) {
            return StreakResult(
                currentStreak = 0,
                longestStreak = calculateLongest(sortedDates),
                lastWorkoutDate = sortedDates.firstOrNull(),
            )
        }

        var currentStreak = 0
        var expectedDate = if (sortedDates.first() == today) today else yesterday

        for (date in sortedDates) {
            if (date == expectedDate) {
                currentStreak++
                expectedDate = expectedDate.minusDays(1)
            } else if (date.isBefore(expectedDate)) {
                break
            }
        }

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = calculateLongest(sortedDates),
            lastWorkoutDate = sortedDates.firstOrNull(),
        )
    }

    private fun calculateLongest(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] == sortedDates[i - 1].minusDays(1)) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }

    data class StreakResult(
        val currentStreak: Int,
        val longestStreak: Int,
        val lastWorkoutDate: LocalDate?,
    )
}
