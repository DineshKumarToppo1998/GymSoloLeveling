package com.hunterxdk.gymsololeveling.feature.exercise.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunterxdk.gymsololeveling.core.domain.model.enums.ExerciseCounterType
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup

private val EQUIPMENT_OPTIONS = listOf(
    "barbell", "dumbbell", "cable", "machine",
    "bodyweight", "kettlebell", "resistance_band", "smith_machine",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomExerciseFormScreen(
    exerciseId: String? = null,
    onSaved: () -> Unit,
    onPickYoutube: (currentVideoId: String?) -> Unit,
    onBack: () -> Unit,
    viewModel: CustomExerciseFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(exerciseId) { exerciseId?.let { viewModel.loadExercise(it) } }
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId == null) "New Exercise" else "Edit Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Exercise Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                )
            }

            item {
                Text("Counter Type", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExerciseCounterType.entries) { type ->
                        FilterChip(
                            selected = uiState.counterType == type,
                            onClick = { viewModel.onCounterTypeChange(type) },
                            label = { Text(type.name.lowercase().replace("_", " ")) },
                        )
                    }
                }
            }

            item {
                Text("Primary Muscles", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MuscleGroup.entries) { muscle ->
                        FilterChip(
                            selected = muscle.name in uiState.primaryMuscles,
                            onClick = { viewModel.togglePrimaryMuscle(muscle.name) },
                            label = { Text(muscle.displayName) },
                        )
                    }
                }
            }

            item {
                Text("Secondary Muscles", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MuscleGroup.entries) { muscle ->
                        FilterChip(
                            selected = muscle.name in uiState.secondaryMuscles,
                            onClick = { viewModel.toggleSecondaryMuscle(muscle.name) },
                            label = { Text(muscle.displayName) },
                        )
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = uiState.equipmentDropdownExpanded,
                    onExpandedChange = viewModel::setEquipmentDropdownExpanded,
                ) {
                    OutlinedTextField(
                        value = uiState.mainEquipment
                            .replace("_", " ")
                            .replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipment") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = uiState.equipmentDropdownExpanded,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = uiState.equipmentDropdownExpanded,
                        onDismissRequest = { viewModel.setEquipmentDropdownExpanded(false) },
                    ) {
                        EQUIPMENT_OPTIONS.forEach { equipment ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        equipment.replace("_", " ")
                                            .replaceFirstChar { it.uppercase() },
                                    )
                                },
                                onClick = {
                                    viewModel.onEquipmentChange(equipment)
                                    viewModel.setEquipmentDropdownExpanded(false)
                                },
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Difficulty: ${
                        when (uiState.difficulty) {
                            1 -> "Easy"
                            2 -> "Medium"
                            else -> "Hard"
                        }
                    }",
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = uiState.difficulty.toFloat(),
                    onValueChange = { viewModel.onDifficultyChange(it.toInt()) },
                    valueRange = 1f..3f,
                    steps = 1,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.instructions,
                    onValueChange = viewModel::onInstructionsChange,
                    label = { Text("Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
            }

            item {
                Card(
                    onClick = { onPickYoutube(uiState.youtubeVideoId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = if (uiState.youtubeVideoId != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (uiState.youtubeVideoId != null) "Video Attached" else "Add YouTube Video",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            uiState.youtubeVideoId?.let { id ->
                                Text(
                                    id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = uiState.name.isNotBlank() && uiState.primaryMuscles.isNotEmpty(),
                ) {
                    Text("Save Exercise")
                }
            }
        }
    }
}
