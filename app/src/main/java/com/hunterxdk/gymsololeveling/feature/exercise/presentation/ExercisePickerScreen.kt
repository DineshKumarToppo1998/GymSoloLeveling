package com.hunterxdk.gymsololeveling.feature.exercise.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hunterxdk.gymsololeveling.core.domain.model.Exercise
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    onExerciseSelected: (Exercise) -> Unit,
    onBack: () -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Add Exercise") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                MuscleFilterChips(onMuscleSelected = viewModel::onMuscleFilter)
                EquipmentFilterChips(onEquipmentSelected = viewModel::onEquipmentFilter)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(exercises, key = { it.id }) { exercise ->
                ExerciseListItem(
                    exercise = exercise,
                    onClick = { onExerciseSelected(exercise) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            if (exercises.isEmpty()) {
                item {
                    Box(
                        Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏋️", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No exercises found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search exercises...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                }
            }
        } else null,
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleFilterChips(onMuscleSelected: (String?) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MuscleGroup.entries.toList()) { muscle ->
            FilterChip(
                selected = selected == muscle.name,
                onClick = {
                    selected = if (selected == muscle.name) null else muscle.name
                    onMuscleSelected(selected)
                },
                label = { Text(muscle.displayName) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentFilterChips(onEquipmentSelected: (String?) -> Unit) {
    val equipmentOptions = listOf(
        "barbell", "dumbbell", "cable", "machine", "bodyweight", "kettlebell", "resistance_band"
    )
    var selected by remember { mutableStateOf<String?>(null) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(equipmentOptions) { equip ->
            FilterChip(
                selected = selected == equip,
                onClick = {
                    selected = if (selected == equip) null else equip
                    onEquipmentSelected(selected)
                },
                label = { Text(equip.replace("_", " ").replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
fun ExerciseListItem(exercise: Exercise, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                "${exercise.primaryMuscles.take(2).joinToString(", ")} • ${exercise.mainEquipment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            DifficultyBadge(difficulty = exercise.difficulty)
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun DifficultyBadge(difficulty: Int) {
    val (color, label) = when (difficulty) {
        1 -> MaterialTheme.colorScheme.secondary to "Easy"
        2 -> Color(0xFFFF9800) to "Med"
        else -> MaterialTheme.colorScheme.error to "Hard"
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}