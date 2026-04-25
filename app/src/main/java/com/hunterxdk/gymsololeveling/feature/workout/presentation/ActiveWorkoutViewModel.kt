package com.hunterxdk.gymsololeveling.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutExercise
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSession
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSet
import com.hunterxdk.gymsololeveling.core.domain.model.effectiveUserId
import com.hunterxdk.gymsololeveling.core.domain.model.enums.ExerciseCounterType
import com.hunterxdk.gymsololeveling.core.domain.model.enums.WorkoutType
import com.hunterxdk.gymsololeveling.feature.workout.data.WorkoutPersistenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val persistenceManager: WorkoutPersistenceManager,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _session = MutableStateFlow<WorkoutSession?>(null)
    val session: StateFlow<WorkoutSession?> = _session.asStateFlow()

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _hasResumableSession = MutableStateFlow(false)
    val hasResumableSession: StateFlow<Boolean> = _hasResumableSession.asStateFlow()

    private var restTimerJob: Job? = null

    init {
        viewModelScope.launch { checkForResumableSession() }
    }

    private suspend fun checkForResumableSession() {
        val saved = persistenceManager.getResumableSession()
        if (saved != null) _hasResumableSession.value = true
    }

    fun startNewWorkout(title: String, workoutType: WorkoutType) {
        viewModelScope.launch {
            val userId = sessionManager.session.first().effectiveUserId
            val session = WorkoutSession(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                workoutType = workoutType,
                startedAt = System.currentTimeMillis(),
            )
            _session.value = session
            persistenceManager.startSession(session)
        }
    }

    fun addExercise(exercise: Exercise) {
        val current = _session.value ?: return
        val workoutExercise = WorkoutExercise(
            id = UUID.randomUUID().toString(),
            sessionId = current.id,
            exercise = exercise,
            orderIndex = current.exercises.size,
        )
        _session.update { it?.copy(exercises = it.exercises + workoutExercise) }
        viewModelScope.launch { persistenceManager.addExercise(workoutExercise) }
    }

    fun logSet(workoutExerciseId: String, setData: WorkoutSetData) {
        val current = _session.value ?: return
        val exerciseIndex = current.exercises.indexOfFirst { it.id == workoutExerciseId }
        if (exerciseIndex < 0) return

        val exercise = current.exercises[exerciseIndex]
        val isPR = detectPersonalRecord(exercise, setData)
        val set = setData.toWorkoutSet(
            id = UUID.randomUUID().toString(),
            workoutExerciseId = workoutExerciseId,
            setNumber = exercise.sets.size + 1,
            isPersonalRecord = isPR,
        )

        val updatedExercises = current.exercises.toMutableList().also {
            it[exerciseIndex] = exercise.copy(sets = exercise.sets + set)
        }
        _session.update { it?.copy(exercises = updatedExercises) }
        viewModelScope.launch { persistenceManager.persistSet(set) }

        if (isPR) _uiState.update { it.copy(newPR = set) }
        startRestTimer(setData.recommendedRestSeconds)
    }

    fun deleteSet(workoutExerciseId: String, setId: String) {
        val current = _session.value ?: return
        val exerciseIndex = current.exercises.indexOfFirst { it.id == workoutExerciseId }
        if (exerciseIndex < 0) return
        val exercise = current.exercises[exerciseIndex]
        val updatedExercises = current.exercises.toMutableList().also {
            it[exerciseIndex] = exercise.copy(sets = exercise.sets.filter { s -> s.id != setId })
        }
        _session.update { it?.copy(exercises = updatedExercises) }
        viewModelScope.launch { persistenceManager.deleteSet(setId) }
    }

    fun dismissPR() {
        _uiState.update { it.copy(newPR = null) }
    }

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _restTimerSeconds.value = seconds
        restTimerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                delay(1000)
                _restTimerSeconds.update { it - 1 }
            }
        }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _restTimerSeconds.value = 0
    }

    fun discardWorkout() {
        restTimerJob?.cancel()
        viewModelScope.launch { persistenceManager.clearSession() }
        _session.value = null
        _hasResumableSession.value = false
    }

    fun dismissResumable() {
        _hasResumableSession.value = false
    }

    private fun detectPersonalRecord(exercise: WorkoutExercise, setData: WorkoutSetData): Boolean {
        val newVolume = (setData.reps ?: 0) * (setData.weightKg ?: 0.0)
        if (newVolume <= 0) return false
        val existingMax = exercise.sets.maxOfOrNull { (it.reps ?: 0) * (it.weightKg ?: 0.0) } ?: 0.0
        return newVolume > existingMax
    }
}

data class ActiveWorkoutUiState(
    val newPR: WorkoutSet? = null,
    val error: String? = null,
)

sealed class WorkoutSetData {
    abstract val recommendedRestSeconds: Int
    open val reps: Int? get() = null
    open val weightKg: Double? get() = null

    data class RepsAndWeight(override val reps: Int, override val weightKg: Double, override val recommendedRestSeconds: Int = 90) : WorkoutSetData()
    data class RepsOnly(override val reps: Int, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()
    data class Bodyweight(override val reps: Int, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()
    data class TimeOnly(val durationSeconds: Int, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()
    data class TimeAndDistanceAndIncline(val durationSeconds: Int, val distanceKm: Double, val inclinePercent: Double, override val recommendedRestSeconds: Int = 120) : WorkoutSetData()
    data class TimeAndDistanceAndResistance(val durationSeconds: Int, val distanceKm: Double, val resistanceLevel: Int, override val recommendedRestSeconds: Int = 120) : WorkoutSetData()
    data class TimeAndFloorsAndSteps(val durationSeconds: Int, val floors: Int, val steps: Int, override val recommendedRestSeconds: Int = 90) : WorkoutSetData()
    data class ResistanceBand(override val reps: Int, val bandResistance: String, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()
    data class ResistanceBandTime(val durationSeconds: Int, val bandResistance: String, override val recommendedRestSeconds: Int = 60) : WorkoutSetData()

    fun toWorkoutSet(id: String, workoutExerciseId: String, setNumber: Int, isPersonalRecord: Boolean): WorkoutSet = when (this) {
        is RepsAndWeight -> WorkoutSet(id, workoutExerciseId, setNumber, reps = reps, weightKg = weightKg, isPersonalRecord = isPersonalRecord)
        is RepsOnly -> WorkoutSet(id, workoutExerciseId, setNumber, reps = reps, isPersonalRecord = isPersonalRecord)
        is Bodyweight -> WorkoutSet(id, workoutExerciseId, setNumber, reps = reps, isPersonalRecord = isPersonalRecord)
        is TimeOnly -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, isPersonalRecord = isPersonalRecord)
        is TimeAndDistanceAndIncline -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, distanceKm = distanceKm, inclinePercent = inclinePercent)
        is TimeAndDistanceAndResistance -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, distanceKm = distanceKm, resistanceLevel = resistanceLevel)
        is TimeAndFloorsAndSteps -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, floors = floors, steps = steps)
        is ResistanceBand -> WorkoutSet(id, workoutExerciseId, setNumber, reps = reps, bandResistance = bandResistance)
        is ResistanceBandTime -> WorkoutSet(id, workoutExerciseId, setNumber, durationSeconds = durationSeconds, bandResistance = bandResistance)
    }
}
