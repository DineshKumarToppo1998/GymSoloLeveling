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
class InjurySettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {
    val selectedInjuries: StateFlow<Set<String>> = prefs.activeInjuries
        .map { it.split(",").filter { i -> i.isNotBlank() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(id: String) {
        viewModelScope.launch {
            val current = selectedInjuries.value.toMutableSet()
            when {
                id == "none" -> {
                    current.clear()
                    current.add("none")
                }
                "none" in current -> {
                    current.remove("none")
                    if (!current.add(id)) current.remove(id)
                }
                else -> {
                    if (!current.add(id)) current.remove(id)
                }
            }
            prefs.setInjuries(current.joinToString(","))
        }
    }
}