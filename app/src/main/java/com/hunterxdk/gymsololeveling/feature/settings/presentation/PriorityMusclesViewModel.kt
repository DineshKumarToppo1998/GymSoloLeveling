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
class PriorityMusclesViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {
    val selected: StateFlow<Set<String>> = prefs.priorityMuscles
        .map { it.split(",").filter { m -> m.isNotBlank() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(muscleName: String) {
        viewModelScope.launch {
            val current = selected.value.toMutableSet()
            if (!current.add(muscleName)) current.remove(muscleName)
            if (current.size <= 3) prefs.setPriorityMuscles(current.joinToString(","))
        }
    }
}