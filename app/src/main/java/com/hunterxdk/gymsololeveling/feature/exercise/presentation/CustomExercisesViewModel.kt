package com.hunterxdk.gymsololeveling.feature.exercise.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.feature.exercise.data.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomExercisesViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val customExercises: StateFlow<List<Exercise>> = exerciseRepository.getCustomExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch { exerciseRepository.deleteCustomExercise(exercise) }
    }
}
