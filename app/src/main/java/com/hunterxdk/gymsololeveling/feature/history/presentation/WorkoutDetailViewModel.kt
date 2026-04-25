package com.hunterxdk.gymsololeveling.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.feature.history.data.WorkoutDetailData
import com.hunterxdk.gymsololeveling.feature.history.data.WorkoutHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val historyRepository: WorkoutHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(workoutId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val detail = historyRepository.getWorkoutDetail(workoutId)
            _uiState.update { it.copy(isLoading = false, workout = detail) }
        }
    }
}

data class WorkoutDetailUiState(
    val isLoading: Boolean = false,
    val workout: WorkoutDetailData? = null,
)
