package com.hunterxdk.gymsololeveling.feature.templates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hunterxdk.gymsololeveling.core.data.local.dao.WorkoutTemplateDao
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutTemplate
import com.hunterxdk.gymsololeveling.feature.templates.data.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SavedWorkoutsViewModel @Inject constructor(
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val json: Json,
) : ViewModel() {

    val templates: StateFlow<List<WorkoutTemplate>> = flowOf(
        FirebaseAuth.getInstance().currentUser?.uid
    ).flatMapLatest { uid ->
        if (uid == null) emptyFlow()
        else workoutTemplateDao.getAll(uid).map { entities ->
            entities.map { it.toDomain(json) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
