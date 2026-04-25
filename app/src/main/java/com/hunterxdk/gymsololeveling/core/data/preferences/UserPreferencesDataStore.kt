package com.hunterxdk.gymsololeveling.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    val isGuestMode: Flow<Boolean> = store.data.map { it[Keys.GUEST_MODE] ?: false }
    val localUserId: Flow<String> = store.data.map { it[Keys.LOCAL_USER_ID] ?: "" }
    val hasCompletedOnboarding: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val isDarkTheme: Flow<Boolean> = store.data.map { it[Keys.DARK_THEME] ?: true }
    val preferredUnit: Flow<String> = store.data.map { it[Keys.UNIT] ?: "kg" }
    val workoutReminderEnabled: Flow<Boolean> = store.data.map { it[Keys.REMINDER_ENABLED] ?: false }
    val workoutReminderHour: Flow<Int> = store.data.map { it[Keys.REMINDER_HOUR] ?: 7 }
    val workoutReminderMinute: Flow<Int> = store.data.map { it[Keys.REMINDER_MINUTE] ?: 0 }
    val challengeDifficulty: Flow<String> = store.data.map { it[Keys.CHALLENGE_DIFFICULTY] ?: "MEDIUM" }
    val availableEquipment: Flow<String> = store.data.map { it[Keys.EQUIPMENT] ?: "" }
    val preferredWorkoutDays: Flow<String> = store.data.map { it[Keys.WORKOUT_DAYS] ?: "" }
    val priorityMuscles: Flow<String> = store.data.map { it[Keys.PRIORITY_MUSCLES] ?: "" }
    val playerClass: Flow<String> = store.data.map { it[Keys.PLAYER_CLASS] ?: "" }
    val activeInjuries: Flow<String> = store.data.map { it[Keys.INJURIES] ?: "" }
    val exerciseLocale: Flow<String> = store.data.map { it[Keys.EXERCISE_LOCALE] ?: "" }
    val lastSeedLocale: Flow<String> = store.data.map { it[Keys.LAST_SEED_LOCALE] ?: "" }

    suspend fun setOnboardingDone() = store.edit { it[Keys.ONBOARDING_DONE] = true }
    suspend fun setDarkTheme(dark: Boolean) = store.edit { it[Keys.DARK_THEME] = dark }
    suspend fun setUnit(unit: String) = store.edit { it[Keys.UNIT] = unit }
    suspend fun setReminderEnabled(enabled: Boolean) = store.edit { it[Keys.REMINDER_ENABLED] = enabled }
    suspend fun setReminderTime(hour: Int, minute: Int) = store.edit {
        it[Keys.REMINDER_HOUR] = hour
        it[Keys.REMINDER_MINUTE] = minute
    }
    suspend fun setChallengeDifficulty(d: String) = store.edit { it[Keys.CHALLENGE_DIFFICULTY] = d }
    suspend fun setAvailableEquipment(json: String) = store.edit { it[Keys.EQUIPMENT] = json }
    suspend fun setPreferredWorkoutDays(json: String) = store.edit { it[Keys.WORKOUT_DAYS] = json }
    suspend fun setPriorityMuscles(json: String) = store.edit { it[Keys.PRIORITY_MUSCLES] = json }
    suspend fun setPlayerClass(cls: String) = store.edit { it[Keys.PLAYER_CLASS] = cls }
    suspend fun setInjuries(json: String) = store.edit { it[Keys.INJURIES] = json }
    suspend fun setExerciseLocale(locale: String) = store.edit { it[Keys.EXERCISE_LOCALE] = locale }
    suspend fun setLastSeedLocale(locale: String) = store.edit { it[Keys.LAST_SEED_LOCALE] = locale }
    suspend fun enableGuestMode() = store.edit {
        it[Keys.GUEST_MODE] = true
        if (it[Keys.LOCAL_USER_ID].isNullOrEmpty()) {
            it[Keys.LOCAL_USER_ID] = java.util.UUID.randomUUID().toString()
        }
    }
    suspend fun clearGuestMode() = store.edit { it[Keys.GUEST_MODE] = false }

    private object Keys {
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
        val LOCAL_USER_ID = stringPreferencesKey("local_user_id")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val UNIT = stringPreferencesKey("preferred_unit")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val CHALLENGE_DIFFICULTY = stringPreferencesKey("challenge_difficulty")
        val EQUIPMENT = stringPreferencesKey("available_equipment")
        val WORKOUT_DAYS = stringPreferencesKey("workout_days")
        val PRIORITY_MUSCLES = stringPreferencesKey("priority_muscles")
        val PLAYER_CLASS = stringPreferencesKey("player_class")
        val INJURIES = stringPreferencesKey("injuries")
        val EXERCISE_LOCALE = stringPreferencesKey("exercise_locale")
        val LAST_SEED_LOCALE = stringPreferencesKey("last_seed_locale")
    }
}
