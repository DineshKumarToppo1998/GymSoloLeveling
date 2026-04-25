package com.hunterxdk.gymsololeveling.feature.settings.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import com.hunterxdk.gymsololeveling.core.worker.WorkoutReminderWorker
import com.hunterxdk.gymsololeveling.feature.auth.data.AuthRepository
import com.hunterxdk.gymsololeveling.feature.exercise.data.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsDataStore: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val exerciseRepository: ExerciseRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val isDarkTheme = prefsDataStore.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val preferredUnit = prefsDataStore.preferredUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "kg")

    val reminderEnabled = prefsDataStore.workoutReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val reminderHour = prefsDataStore.workoutReminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 7)

    val reminderMinute = prefsDataStore.workoutReminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val challengeDifficulty = prefsDataStore.challengeDifficulty
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "MEDIUM")

    val exerciseLocale = prefsDataStore.exerciseLocale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _deleteAccountState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteAccountState: StateFlow<DeleteState> = _deleteAccountState.asStateFlow()

    fun setDarkTheme(dark: Boolean) = viewModelScope.launch { prefsDataStore.setDarkTheme(dark) }
    fun setUnit(unit: String) = viewModelScope.launch { prefsDataStore.setUnit(unit) }
    fun setChallengeDifficulty(d: String) = viewModelScope.launch { prefsDataStore.setChallengeDifficulty(d) }
    fun setLocale(locale: String) = viewModelScope.launch {
        prefsDataStore.setExerciseLocale(locale)
        exerciseRepository.seedIfNeeded()
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsDataStore.setReminderEnabled(enabled)
            if (enabled) {
                WorkoutReminderWorker.schedule(context, reminderHour.value, reminderMinute.value)
            } else {
                WorkoutReminderWorker.cancel(context)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsDataStore.setReminderTime(hour, minute)
            if (reminderEnabled.value) {
                WorkoutReminderWorker.schedule(context, hour, minute)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            sessionManager.signOut()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = DeleteState.Loading
            authRepository.deleteAccount()
                .onSuccess { _deleteAccountState.value = DeleteState.Success }
                .onFailure { e -> _deleteAccountState.value = DeleteState.Error(e.message ?: "Failed") }
        }
    }

    sealed class DeleteState {
        data object Idle : DeleteState()
        data object Loading : DeleteState()
        data object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }
}