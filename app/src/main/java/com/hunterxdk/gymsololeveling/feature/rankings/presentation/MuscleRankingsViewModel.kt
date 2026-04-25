package com.hunterxdk.gymsololeveling.feature.rankings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.model.MuscleRank
import com.hunterxdk.gymsololeveling.feature.rankings.data.MuscleRankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MuscleRankingsViewModel @Inject constructor(
    private val muscleRankRepository: MuscleRankRepository,
) : ViewModel() {

    val ranks: StateFlow<List<MuscleRank>> = muscleRankRepository.allRanks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMuscle = MutableStateFlow<MuscleRank?>(null)
    val selectedMuscle: StateFlow<MuscleRank?> = _selectedMuscle.asStateFlow()

    fun onMuscleSelected(muscle: MuscleRank) {
        _selectedMuscle.value = if (_selectedMuscle.value?.muscleGroup == muscle.muscleGroup) null else muscle
    }
}
