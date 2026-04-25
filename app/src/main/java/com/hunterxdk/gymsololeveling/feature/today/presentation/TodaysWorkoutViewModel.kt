package com.hunterxdk.gymsololeveling.feature.today.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.core.service.TodaysWorkoutService
import com.hunterxdk.gymsololeveling.feature.exercise.data.ExerciseRepository
import com.hunterxdk.gymsololeveling.feature.rankings.data.MuscleRankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodaysWorkoutViewModel @Inject constructor(
    private val todaysWorkoutService: TodaysWorkoutService,
    private val muscleRankRepository: MuscleRankRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodaysWorkoutUiState())
    val uiState: StateFlow<TodaysWorkoutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { generateRecommendation() }
    }

    private suspend fun generateRecommendation() {
        _uiState.update { it.copy(isLoading = true) }

        val ranks = muscleRankRepository.allRanks.first()
        val recommendation = todaysWorkoutService.getRecommendation(
            muscleRanks = ranks,
            lastTrainedDates = emptyMap(),
            priorityMuscles = emptyList(),
            availableEquipment = emptyList(),
            preferredDays = emptyList(),
        )

        if (recommendation != null) {
            val exercises = recommendation.targetMuscles.flatMap { muscle ->
                exerciseRepository.getByMuscle(muscle.name.lowercase()).first().take(3)
            }.distinctBy { it.id }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    recommendation = recommendation,
                    suggestedExercises = exercises,
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false, isRestDay = true) }
        }
    }
}

data class TodaysWorkoutUiState(
    val isLoading: Boolean = false,
    val recommendation: TodaysWorkoutService.WorkoutRecommendation? = null,
    val suggestedExercises: List<Exercise> = emptyList(),
    val isRestDay: Boolean = false,
)
