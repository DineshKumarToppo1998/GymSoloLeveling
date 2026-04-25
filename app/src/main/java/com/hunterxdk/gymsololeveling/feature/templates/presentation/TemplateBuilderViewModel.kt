package com.hunterxdk.gymsololeveling.feature.templates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hunterxdk.gymsololeveling.core.data.local.dao.WorkoutTemplateDao
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutExercise
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutTemplate
import com.hunterxdk.gymsololeveling.core.domain.model.enums.WorkoutType
import com.hunterxdk.gymsololeveling.feature.templates.data.toDomain
import com.hunterxdk.gymsololeveling.feature.templates.data.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TemplateBuilderViewModel @Inject constructor(
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateBuilderUiState())
    val uiState: StateFlow<TemplateBuilderUiState> = _uiState.asStateFlow()

    fun loadTemplate(id: String) {
        viewModelScope.launch {
            val entity = workoutTemplateDao.getById(id) ?: return@launch
            val template = entity.toDomain(json)
            _uiState.update {
                it.copy(
                    templateId = template.id,
                    name = template.name,
                    workoutType = template.workoutType,
                    exercises = template.exercises,
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }

    fun onWorkoutTypeChange(type: WorkoutType) = _uiState.update { it.copy(workoutType = type) }

    fun addExercise(exercise: WorkoutExercise) = _uiState.update {
        it.copy(exercises = it.exercises + exercise)
    }

    fun removeExercise(id: String) = _uiState.update {
        it.copy(exercises = it.exercises.filter { ex -> ex.id != id })
    }

    fun save() {
        val state = _uiState.value
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val template = WorkoutTemplate(
                id = state.templateId ?: UUID.randomUUID().toString(),
                userId = uid,
                name = state.name,
                workoutType = state.workoutType,
                exercises = state.exercises,
                estimatedDurationMinutes = (state.exercises.size * 8).coerceAtLeast(15),
                createdAt = System.currentTimeMillis(),
            )
            workoutTemplateDao.upsert(template.toEntity(json))
        }
    }
}

data class TemplateBuilderUiState(
    val templateId: String? = null,
    val name: String = "",
    val workoutType: WorkoutType = WorkoutType.CUSTOM,
    val exercises: List<WorkoutExercise> = emptyList(),
)
