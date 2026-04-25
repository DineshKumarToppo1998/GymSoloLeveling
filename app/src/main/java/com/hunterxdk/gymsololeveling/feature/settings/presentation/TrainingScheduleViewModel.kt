package com.hunterxdk.gymsololeveling.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainingScheduleViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {
    val selectedDays: StateFlow<Set<String>> = prefs.preferredWorkoutDays
        .map { it.split(",").filter { d -> d.isNotBlank() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleDay(day: String) {
        viewModelScope.launch {
            val current = selectedDays.value.toMutableSet()
            if (!current.add(day)) current.remove(day)
            prefs.setPreferredWorkoutDays(current.joinToString(","))
        }
    }
}