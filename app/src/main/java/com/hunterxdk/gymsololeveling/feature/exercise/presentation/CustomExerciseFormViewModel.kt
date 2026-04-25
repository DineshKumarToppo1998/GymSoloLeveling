package com.hunterxdk.gymsololeveling.feature.exercise.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.core.domain.model.counterType
import com.hunterxdk.gymsololeveling.core.domain.model.enums.ExerciseCounterType
import com.hunterxdk.gymsololeveling.feature.exercise.data.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomExerciseFormViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomExerciseFormUiState())
    val uiState: StateFlow<CustomExerciseFormUiState> = _uiState.asStateFlow()

    fun loadExercise(id: String) {
        viewModelScope.launch {
            val exercise = exerciseRepository.getById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    name = exercise.name,
                    counterType = exercise.counterType(),
                    primaryMuscles = exercise.primaryMuscles.toMutableSet(),
                    secondaryMuscles = exercise.secondaryMuscles.toMutableSet(),
                    mainEquipment = exercise.mainEquipment,
                    difficulty = exercise.difficulty,
                    instructions = exercise.instructions.joinToString("\n"),
                    youtubeVideoId = exercise.youtubeVideoId,
                    editingId = id,
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameError = null) }
    fun onCounterTypeChange(type: ExerciseCounterType) = _uiState.update { it.copy(counterType = type) }
    fun togglePrimaryMuscle(muscle: String) = _uiState.update { it.copy(primaryMuscles = it.primaryMuscles.toggle(muscle)) }
    fun toggleSecondaryMuscle(muscle: String) = _uiState.update { it.copy(secondaryMuscles = it.secondaryMuscles.toggle(muscle)) }
    fun onEquipmentChange(eq: String) = _uiState.update { it.copy(mainEquipment = eq) }
    fun onDifficultyChange(d: Int) = _uiState.update { it.copy(difficulty = d) }
    fun onInstructionsChange(text: String) = _uiState.update { it.copy(instructions = text) }
    fun setYoutubeVideoId(id: String?) = _uiState.update { it.copy(youtubeVideoId = id) }
    fun setEquipmentDropdownExpanded(expanded: Boolean) = _uiState.update { it.copy(equipmentDropdownExpanded = expanded) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name required") }
            return
        }
        viewModelScope.launch {
            val exercise = Exercise(
                id = state.editingId ?: java.util.UUID.randomUUID().toString(),
                name = state.name,
                mainEquipment = state.mainEquipment,
                otherEquipment = emptyList(),
                primaryMuscles = state.primaryMuscles.toList(),
                secondaryMuscles = state.secondaryMuscles.toList(),
                splitCategories = emptyList(),
                exerciseCounterType = state.counterType.name.lowercase(),
                exerciseMechanics = "compound",
                difficulty = state.difficulty,
                instructions = state.instructions.lines().filter { it.isNotBlank() },
                tips = emptyList(),
                benefits = emptyList(),
                breathingInstructions = "",
                keywords = listOf(state.name.lowercase()),
                metabolicEquivalent = 4.0,
                repSupplement = null,
                isCustom = true,
                youtubeVideoId = state.youtubeVideoId,
            )
            exerciseRepository.saveCustomExercise(exercise)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun Set<String>.toggle(item: String): MutableSet<String> =
        toMutableSet().also { if (item in it) it.remove(item) else it.add(item) }
}

data class CustomExerciseFormUiState(
    val name: String = "",
    val nameError: String? = null,
    val counterType: ExerciseCounterType = ExerciseCounterType.REPS_AND_WEIGHT,
    val primaryMuscles: Set<String> = emptySet(),
    val secondaryMuscles: Set<String> = emptySet(),
    val mainEquipment: String = "dumbbell",
    val difficulty: Int = 1,
    val instructions: String = "",
    val youtubeVideoId: String? = null,
    val equipmentDropdownExpanded: Boolean = false,
    val editingId: String? = null,
    val isSaved: Boolean = false,
)
