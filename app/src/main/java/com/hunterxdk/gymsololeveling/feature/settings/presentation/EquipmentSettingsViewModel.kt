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
class EquipmentSettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {
    val selectedEquipment: StateFlow<Set<String>> = prefs.availableEquipment
        .map { it.split(",").filter { e -> e.isNotBlank() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(id: String) {
        viewModelScope.launch {
            val current = selectedEquipment.value.toMutableSet()
            if (!current.add(id)) current.remove(id)
            prefs.setAvailableEquipment(current.joinToString(","))
        }
    }
}