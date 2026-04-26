package com.hunterxdk.gymsololeveling.feature.exercise.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.feature.exercise.data.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMuscle = MutableStateFlow<String?>(null)
    private val _selectedEquipment = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow<String?>(null)

    private val _isSeeding = MutableStateFlow(true)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    val exercises: StateFlow<List<Exercise>> = flow {
        try {
            exerciseRepository.seedIfNeeded()
        } catch (_: Exception) {
            // continue — show whatever is already in DB
        }
        emitAll(
            combine(
                _searchQuery, _selectedMuscle, _selectedEquipment, _selectedCategory
            ) { query, muscle, equipment, category ->
                Triple(query, muscle ?: equipment ?: category, muscle to equipment)
            }.flatMapLatest { (query, _, filters) ->
                val (muscle, equipment) = filters
                when {
                    query.isNotBlank() -> exerciseRepository.searchExercises(query)
                    muscle != null -> exerciseRepository.getByMuscle(muscle)
                    equipment != null -> exerciseRepository.getByEquipment(equipment)
                    else -> exerciseRepository.getAllExercises()
                }
            }
        )
    }.onEach { _isSeeding.value = false }
        .catch { _isSeeding.value = false; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQuery(q: String) { _searchQuery.value = q }
    fun onMuscleFilter(muscle: String?) { _selectedMuscle.value = muscle; _selectedEquipment.value = null }
    fun onEquipmentFilter(equipment: String?) { _selectedEquipment.value = equipment; _selectedMuscle.value = null }
    fun onCategoryFilter(category: String?) { _selectedCategory.value = category }
    fun clearFilters() { _selectedMuscle.value = null; _selectedEquipment.value = null; _selectedCategory.value = null }
}
