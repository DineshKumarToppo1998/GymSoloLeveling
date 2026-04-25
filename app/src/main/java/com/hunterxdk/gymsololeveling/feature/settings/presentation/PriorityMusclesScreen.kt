package com.hunterxdk.gymsololeveling.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup
import com.hunterxdk.gymsololeveling.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityMusclesScreen(
    onBack: () -> Unit,
    viewModel: PriorityMusclesViewModel = hiltViewModel(),
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Priority Muscles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                "Pick up to 3 muscle groups to prioritise in your training plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(MuscleGroup.entries) { muscle ->
                    val isSelected = muscle.name in selected
                    val canSelect = isSelected || selected.size < 3
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (canSelect || isSelected) viewModel.toggle(muscle.name) },
                        label = {
                            Text(
                                muscle.displayName,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        enabled = canSelect || isSelected,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                            selectedLabelColor = GoldAccent,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "${selected.size}/3 selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}