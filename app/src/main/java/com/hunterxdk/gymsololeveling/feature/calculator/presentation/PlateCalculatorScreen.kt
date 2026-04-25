package com.hunterxdk.gymsololeveling.feature.calculator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCalculatorScreen(onBack: () -> Unit) {
    var targetWeight by remember { mutableStateOf("") }
    var barbellWeight by remember { mutableDoubleStateOf(20.0) }
    var unit by remember { mutableStateOf("kg") }

    val targetKg = targetWeight.toDoubleOrNull() ?: 0.0
    val plates = remember(targetKg, barbellWeight, unit) {
        calculatePlates(targetKg, barbellWeight, unit)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plate Calculator") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("kg", "lbs").forEach { u ->
                    FilterChip(
                        selected = unit == u,
                        onClick = { unit = u },
                        label = { Text(u) },
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Bar:", style = MaterialTheme.typography.bodyMedium)
                listOf(15.0, 20.0, 25.0).forEach { barWeight ->
                    FilterChip(
                        selected = barbellWeight == barWeight,
                        onClick = { barbellWeight = barWeight },
                        label = { Text("${barWeight.toInt()}$unit") },
                    )
                }
            }

            OutlinedTextField(
                value = targetWeight,
                onValueChange = { targetWeight = it },
                label = { Text("Target Weight ($unit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (plates.isNotEmpty()) {
                Text("Plates per side:", style = MaterialTheme.typography.titleSmall)
                BarbellDiagram(plates = plates, unit = unit)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        plates.groupBy { it }.forEach { (plate, instances) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("${plate}$unit plate", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "× ${instances.size * 2} (${instances.size} per side)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${barbellWeight + plates.sum() * 2}$unit",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            } else if (targetKg > 0 && targetKg <= barbellWeight) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        "Target weight is less than or equal to barbell weight (${barbellWeight}$unit). No plates needed.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BarbellDiagram(plates: List<Double>, unit: String) {
    val plateColors = mapOf(
        25.0 to Color(0xFFE53935), 20.0 to Color(0xFF1E88E5),
        15.0 to Color(0xFFFFEB3B), 10.0 to Color(0xFF43A047),
        5.0 to Color(0xFFFFFFFF), 2.5 to Color(0xFF9C27B0),
        1.25 to Color(0xFFFF9800), 1.0 to Color(0xFF00BCD4),
        // lbs
        45.0 to Color(0xFFE53935), 35.0 to Color(0xFF1E88E5),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        plates.reversed().forEach { plate ->
            PlateDisc(weight = plate, color = plateColors[plate] ?: Color.Gray, unit = unit)
        }
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(12.dp)
                .background(Color(0xFF757575), RoundedCornerShape(6.dp)),
        )
        plates.forEach { plate ->
            PlateDisc(weight = plate, color = plateColors[plate] ?: Color.Gray, unit = unit)
        }
    }
}

@Composable
private fun PlateDisc(weight: Double, color: Color, unit: String) {
    val height: Dp = when {
        weight >= 20 -> 56.dp
        weight >= 10 -> 48.dp
        weight >= 5 -> 40.dp
        else -> 32.dp
    }
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(height)
            .background(color, RoundedCornerShape(3.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$weight",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = TextUnit(8f, TextUnitType.Sp),
            ),
            color = if (color == Color(0xFFFFFFFF) || color == Color(0xFFFFEB3B)) Color.Black else Color.White,
        )
    }
}

private fun calculatePlates(targetKg: Double, barbellWeight: Double, unit: String): List<Double> {
    if (targetKg <= barbellWeight) return emptyList()

    val availablePlates = if (unit == "kg") {
        listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25, 1.0)
    } else {
        listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    }

    var remaining = (targetKg - barbellWeight) / 2
    val result = mutableListOf<Double>()

    for (plate in availablePlates) {
        while (remaining >= plate - 0.001) {
            result.add(plate)
            remaining -= plate
        }
    }

    return result
}
