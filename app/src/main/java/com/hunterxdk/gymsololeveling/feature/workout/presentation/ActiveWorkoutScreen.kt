package com.hunterxdk.gymsololeveling.feature.workout.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutExercise
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSession
import com.hunterxdk.gymsololeveling.core.domain.model.WorkoutSet
import com.hunterxdk.gymsololeveling.core.domain.model.enums.ExerciseCounterType
import com.hunterxdk.gymsololeveling.core.domain.model.enums.WorkoutType
import com.hunterxdk.gymsololeveling.core.domain.model.counterType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onFinishWorkout: (WorkoutSession) -> Unit,
    onAddExercise: (sessionId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasResumable by viewModel.hasResumableSession.collectAsStateWithLifecycle()

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showWorkoutTypeSheet by remember { mutableStateOf(true) }

    if (hasResumable) {
        ResumeWorkoutDialog(
            onResume = { viewModel.dismissResumable() },
            onDiscard = { viewModel.discardWorkout() },
        )
    }

    if (showWorkoutTypeSheet && session == null && !hasResumable) {
        WorkoutTypePickerSheet(
            onSelected = { type ->
                viewModel.startNewWorkout(type.displayName, type)
                showWorkoutTypeSheet = false
            },
            onDismiss = onBack,
        )
    }

    uiState.newPR?.let { pr ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPR() },
            title = { Text("🏆 New Personal Record!") },
            text = { Text("${pr.toDisplayString()} — best set ever for this exercise!") },
            confirmButton = { Button(onClick = { viewModel.dismissPR() }) { Text("Nice!") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        session?.title ?: "New Workout",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { session?.let { onFinishWorkout(it) } },
                        enabled = session != null && session!!.exercises.any { it.sets.isNotEmpty() },
                    ) {
                        Text("Finish", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(visible = restTimer > 0) {
                RestTimerBanner(seconds = restTimer, onSkip = viewModel::skipRestTimer)
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                session?.exercises?.forEach { exercise ->
                    item(key = exercise.id) {
                        WorkoutExerciseCard(
                            workoutExercise = exercise,
                            onLogSet = { setData -> viewModel.logSet(exercise.id, setData) },
                            onDeleteSet = { setId -> viewModel.deleteSet(exercise.id, setId) },
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = { session?.let { onAddExercise(it.id) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = session != null,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Exercise")
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("All logged sets will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardWorkout()
                    onBack()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun ResumeWorkoutDialog(onResume: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Resume Workout?") },
        text = { Text("You have an unfinished workout. Would you like to continue?") },
        confirmButton = { Button(onClick = onResume) { Text("Resume") } },
        dismissButton = { OutlinedButton(onClick = onDiscard) { Text("Start New") } },
    )
}

@Composable
fun RestTimerBanner(seconds: Int, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Rest: ${seconds}s",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    }
}

@Composable
fun WorkoutExerciseCard(
    workoutExercise: WorkoutExercise,
    onLogSet: (WorkoutSetData) -> Unit,
    onDeleteSet: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                workoutExercise.exercise.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (workoutExercise.sets.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                workoutExercise.sets.forEachIndexed { index, set ->
                    LoggedSetRow(
                        setNumber = index + 1,
                        set = set,
                        onDelete = { onDeleteSet(set.id) },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }

            SetInputForm(
                counterType = workoutExercise.exercise.counterType(),
                setNumber = workoutExercise.sets.size + 1,
                onLogSet = onLogSet,
            )
        }
    }
}

@Composable
fun LoggedSetRow(setNumber: Int, set: WorkoutSet, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Set $setNumber",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(48.dp),
        )
        Text(
            set.toDisplayString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (set.isPersonalRecord) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (set.isPersonalRecord) {
            Text(
                "🏆 PR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete set",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

fun WorkoutSet.toDisplayString(): String = buildString {
    reps?.let { append("$it reps") }
    weightKg?.let { if (it > 0) append(" × ${it}kg") }
    durationSeconds?.let { append("${it}s") }
    distanceKm?.let { if (it > 0) append(" ${it}km") }
    inclinePercent?.let { if (it > 0) append(" ${it}%") }
    resistanceLevel?.let { if (it > 0) append(" Lvl $it") }
    floors?.let { if (it > 0) append(" $it floors") }
    steps?.let { if (it > 0) append(" $it steps") }
    bandResistance?.let { append(" $it") }
}

@Composable
fun SetInputForm(
    counterType: ExerciseCounterType,
    setNumber: Int,
    onLogSet: (WorkoutSetData) -> Unit,
) {
    when (counterType) {
        ExerciseCounterType.REPS_AND_WEIGHT -> RepsAndWeightForm(setNumber, onLogSet)
        ExerciseCounterType.REPS,
        ExerciseCounterType.REPS_ONLY -> RepsOnlyForm(setNumber, onLogSet)
        ExerciseCounterType.BODYWEIGHT -> BodyweightForm(setNumber, onLogSet)
        ExerciseCounterType.TIME,
        ExerciseCounterType.TIME_ONLY,
        ExerciseCounterType.TIME_AND_SETS -> TimeForm(setNumber, onLogSet)
        ExerciseCounterType.TIME_AND_DISTANCE_AND_INCLINE -> TimeDistanceInclineForm(setNumber, onLogSet)
        ExerciseCounterType.TIME_AND_DISTANCE_AND_RESISTANCE -> TimeDistanceResistanceForm(setNumber, onLogSet)
        ExerciseCounterType.TIME_AND_FLOORS_AND_STEPS -> TimeFloorsStepsForm(setNumber, onLogSet)
        ExerciseCounterType.RESISTANCE_BAND_STRENGTH -> ResistanceBandForm(setNumber, onLogSet)
        ExerciseCounterType.RESISTANCE_BAND_STRENGTH_AND_TIME -> ResistanceBandTimeForm(setNumber, onLogSet)
    }
}

@Composable
private fun RepsAndWeightForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = reps, onValueChange = { reps = it },
            label = { Text("Reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f), singleLine = true,
        )
        OutlinedTextField(
            value = weight, onValueChange = { weight = it },
            label = { Text("kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f), singleLine = true,
        )
        IconButton(onClick = {
            val r = reps.toIntOrNull() ?: return@IconButton
            val w = weight.toDoubleOrNull() ?: 0.0
            onLog(WorkoutSetData.RepsAndWeight(r, w))
            reps = ""; weight = ""
        }) {
            Icon(Icons.Default.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RepsOnlyForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var reps by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = reps, onValueChange = { reps = it },
            label = { Text("Reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f), singleLine = true,
        )
        IconButton(onClick = {
            val r = reps.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.RepsOnly(r))
            reps = ""
        }) {
            Icon(Icons.Default.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BodyweightForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var reps by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = reps, onValueChange = { reps = it },
            label = { Text("Reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f), singleLine = true,
        )
        IconButton(onClick = {
            val r = reps.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.Bodyweight(r))
            reps = ""
        }) {
            Icon(Icons.Default.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TimeForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var duration by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = duration, onValueChange = { duration = it },
            label = { Text("Seconds") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f), singleLine = true,
        )
        IconButton(onClick = {
            val d = duration.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.TimeOnly(d))
            duration = ""
        }) {
            Icon(Icons.Default.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TimeDistanceInclineForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var incline by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Set $setNumber", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(duration, { duration = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(distance, { distance = it }, label = { Text("km") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(incline, { incline = it }, label = { Text("%") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            IconButton(onClick = {
                val d = duration.toIntOrNull() ?: return@IconButton
                onLog(WorkoutSetData.TimeAndDistanceAndIncline(d, distance.toDoubleOrNull() ?: 0.0, incline.toDoubleOrNull() ?: 0.0))
                duration = ""; distance = ""; incline = ""
            }) { Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun TimeDistanceResistanceForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var resistance by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Set $setNumber", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(duration, { duration = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(distance, { distance = it }, label = { Text("km") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(resistance, { resistance = it }, label = { Text("Lvl") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            IconButton(onClick = {
                val d = duration.toIntOrNull() ?: return@IconButton
                onLog(WorkoutSetData.TimeAndDistanceAndResistance(d, distance.toDoubleOrNull() ?: 0.0, resistance.toIntOrNull() ?: 1))
                duration = ""; distance = ""; resistance = ""
            }) { Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun TimeFloorsStepsForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    var duration by remember { mutableStateOf("") }
    var floors by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Set $setNumber", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(duration, { duration = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(floors, { floors = it }, label = { Text("Floors") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(steps, { steps = it }, label = { Text("Steps") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            IconButton(onClick = {
                val d = duration.toIntOrNull() ?: return@IconButton
                onLog(WorkoutSetData.TimeAndFloorsAndSteps(d, floors.toIntOrNull() ?: 0, steps.toIntOrNull() ?: 0))
                duration = ""; floors = ""; steps = ""
            }) { Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResistanceBandForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    val bandOptions = listOf("Light", "Medium", "Heavy", "X-Heavy")
    var reps by remember { mutableStateOf("") }
    var band by remember { mutableStateOf(bandOptions[1]) }
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = reps, onValueChange = { reps = it },
            label = { Text("Reps") },
            modifier = Modifier.weight(1f), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = band,
                onValueChange = {},
                readOnly = true,
                label = { Text("Band") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                bandOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { band = option; expanded = false },
                    )
                }
            }
        }
        IconButton(onClick = {
            val r = reps.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.ResistanceBand(r, band))
            reps = ""
        }) {
            Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResistanceBandTimeForm(setNumber: Int, onLog: (WorkoutSetData) -> Unit) {
    val bandOptions = listOf("Light", "Medium", "Heavy", "X-Heavy")
    var duration by remember { mutableStateOf("") }
    var band by remember { mutableStateOf(bandOptions[1]) }
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set $setNumber", Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = duration, onValueChange = { duration = it },
            label = { Text("Sec") },
            modifier = Modifier.weight(1f), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = band,
                onValueChange = {},
                readOnly = true,
                label = { Text("Band") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                bandOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { band = option; expanded = false },
                    )
                }
            }
        }
        IconButton(onClick = {
            val d = duration.toIntOrNull() ?: return@IconButton
            onLog(WorkoutSetData.ResistanceBandTime(d, band))
            duration = ""
        }) {
            Icon(Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTypePickerSheet(
    onSelected: (WorkoutType) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Select Workout Type", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            WorkoutType.entries.forEach { type ->
                Card(
                    onClick = { onSelected(type) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        type.displayName,
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
