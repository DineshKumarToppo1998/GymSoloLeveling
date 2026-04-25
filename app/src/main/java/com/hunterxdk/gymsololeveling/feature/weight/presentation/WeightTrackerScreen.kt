package com.hunterxdk.gymsololeveling.feature.weight.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunterxdk.gymsololeveling.core.data.local.entity.WeightEntryEntity
import com.hunterxdk.gymsololeveling.ui.theme.GoldAccent
import com.hunterxdk.gymsololeveling.ui.theme.XPGreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackerScreen(
    onBack: () -> Unit,
    viewModel: WeightTrackerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Tracker") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showDialog,
                containerColor = GoldAccent,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (uiState.entries.size >= 2) {
                WeightLineChart(
                    entries = uiState.entries.sortedBy { it.loggedAt },
                    unit = uiState.unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp),
                )
            }

            LazyColumn {
                items(uiState.entries, key = { it.id }) { entry ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.deleteEntry(entry)
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        },
                    ) {
                        ListItem(
                            headlineContent = {
                                val display = if (uiState.unit == "lbs") entry.weightKg / 0.453592 else entry.weightKg
                                Text("%.1f %s".format(display, uiState.unit))
                            },
                            supportingContent = {
                                Text(
                                    Instant.ofEpochMilli(entry.loggedAt)
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideDialog,
            title = { Text("Log Weight") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.inputWeight,
                        onValueChange = viewModel::onWeightInput,
                        label = { Text("Weight (${uiState.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = uiState.inputNotes,
                        onValueChange = viewModel::onNotesInput,
                        label = { Text("Notes (optional)") },
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::logWeight) { Text("Log") } },
            dismissButton = { TextButton(onClick = viewModel::hideDialog) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WeightLineChart(
    entries: List<WeightEntryEntity>,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = XPGreen

    Canvas(modifier = modifier) {
        val weights = entries.map { if (unit == "lbs") it.weightKg / 0.453592 else it.weightKg }
        val minW = weights.minOrNull() ?: return@Canvas
        val maxW = weights.maxOrNull() ?: return@Canvas
        val range = (maxW - minW).coerceAtLeast(1.0)

        val points = entries.mapIndexed { i, _ ->
            val x = if (entries.size > 1) i.toFloat() / (entries.size - 1) * size.width else size.width / 2
            val y = (1f - ((weights[i] - minW) / range).toFloat()) * size.height
            Offset(x, y)
        }

        if (points.size < 2) return@Canvas

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        points.forEach { drawCircle(lineColor, radius = 5.dp.toPx(), center = it) }
    }
}