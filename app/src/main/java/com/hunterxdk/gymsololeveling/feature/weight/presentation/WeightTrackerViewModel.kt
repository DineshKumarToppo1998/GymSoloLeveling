package com.hunterxdk.gymsololeveling.feature.weight.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.data.local.entity.WeightEntryEntity
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import com.hunterxdk.gymsololeveling.feature.weight.data.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeightTrackerUiState(
    val entries: List<WeightEntryEntity> = emptyList(),
    val showAddDialog: Boolean = false,
    val inputWeight: String = "",
    val inputNotes: String = "",
    val unit: String = "kg",
)

@HiltViewModel
class WeightTrackerViewModel @Inject constructor(
    private val weightRepository: WeightRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeightTrackerUiState())
    val uiState: StateFlow<WeightTrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            weightRepository.getEntries().collect { entries ->
                _uiState.update { it.copy(entries = entries) }
            }
        }
        viewModelScope.launch {
            prefs.preferredUnit.collect { unit ->
                _uiState.update { it.copy(unit = unit) }
            }
        }
    }

    fun showDialog() = _uiState.update { it.copy(showAddDialog = true) }
    fun hideDialog() = _uiState.update { it.copy(showAddDialog = false, inputWeight = "", inputNotes = "") }
    fun onWeightInput(w: String) = _uiState.update { it.copy(inputWeight = w) }
    fun onNotesInput(n: String) = _uiState.update { it.copy(inputNotes = n) }

    fun logWeight() {
        val kg = _uiState.value.inputWeight.toDoubleOrNull() ?: return
        val actualKg = if (_uiState.value.unit == "lbs") kg * 0.453592 else kg
        viewModelScope.launch {
            weightRepository.logWeight(actualKg, _uiState.value.inputNotes.takeIf { it.isNotBlank() })
            hideDialog()
        }
    }

    fun deleteEntry(entry: WeightEntryEntity) {
        viewModelScope.launch { weightRepository.deleteEntry(entry) }
    }
}