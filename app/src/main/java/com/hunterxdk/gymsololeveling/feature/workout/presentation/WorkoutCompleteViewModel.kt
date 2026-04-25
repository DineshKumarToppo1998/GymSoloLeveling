package com.hunterxdk.gymsololeveling.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.service.XPService
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSession
import com.hunterxdk.gymsololeveling.feature.workout.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutCompleteViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutCompleteUiState())
    val uiState: StateFlow<WorkoutCompleteUiState> = _uiState.asStateFlow()

    fun processWorkout(session: WorkoutSession) {
        if (_uiState.value.xpResult != null || _uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val exerciseLookup = session.exercises.associate { it.exercise.id to it.exercise }
            val xpResult = XPService.calculateWorkoutXP(session, exerciseLookup)
            workoutRepository.saveCompletedWorkout(session, xpResult)
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false, xpResult = xpResult, session = session) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false, error = e.message) }
                }
        }
    }
}

data class WorkoutCompleteUiState(
    val isProcessing: Boolean = false,
    val session: WorkoutSession? = null,
    val xpResult: XPService.WorkoutXPResult? = null,
    val error: String? = null,
)
