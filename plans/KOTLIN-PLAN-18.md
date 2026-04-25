# KOTLIN-PLAN-18.md — Plate Calculator, Loading Skeletons, Empty/Error States & Offline Banner

## Goal
Implement the Plate Calculator utility, loading skeleton composables for all major screens, consistent empty/error states, and an offline connectivity banner.

## Phase
Polish — Phase 6 of 6. Depends on all previous plans.

---

## Files to Create

### `feature/calculator/presentation/PlateCalculatorScreen.kt`
```kotlin
package com.example.gymlevels.feature.calculator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymlevels.core.theme.GoldAccent

@Composable
fun PlateCalculatorScreen(onBack: () -> Unit) {
    var targetWeight by remember { mutableStateOf("") }
    var barbellWeight by remember { mutableDoubleStateOf(20.0) } // standard 20kg barbell
    var unit by remember { mutableStateOf("kg") }

    val targetKg = targetWeight.toDoubleOrNull() ?: 0.0
    val plates = remember(targetKg, barbellWeight, unit) {
        calculatePlates(targetKg, barbellWeight, unit)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plate Calculator") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Unit toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("kg", "lbs").forEach { u ->
                    FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                }
            }

            // Barbell weight selector
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bar:", style = MaterialTheme.typography.bodyMedium)
                listOf(15.0, 20.0, 25.0).forEach { barWeight ->
                    FilterChip(
                        selected = barbellWeight == barWeight,
                        onClick = { barbellWeight = barWeight },
                        label = { Text("${barWeight.toInt()}$unit") }
                    )
                }
            }

            // Target weight input
            OutlinedTextField(
                value = targetWeight,
                onValueChange = { targetWeight = it },
                label = { Text("Target Weight ($unit)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Barbell diagram
            if (plates.isNotEmpty()) {
                Text("Plates per side:", style = MaterialTheme.typography.titleSmall)
                BarbellDiagram(plates = plates, unit = unit)

                Spacer(Modifier.height(8.dp))

                // Plate list
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        plates.groupBy { it }.forEach { (plate, instances) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${plate}$unit plate", style = MaterialTheme.typography.bodyMedium)
                                Text("× ${instances.size * 2} (${instances.size} per side)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${barbellWeight + plates.sum() * 2}$unit",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else if (targetKg > 0 && targetKg <= barbellWeight) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "Target weight is less than or equal to barbell weight (${barbellWeight}$unit). No plates needed.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun BarbellDiagram(plates: List<Double>, unit: String) {
    val plateColors = mapOf(
        25.0 to Color(0xFFE53935), 20.0 to Color(0xFF1E88E5),
        15.0 to Color(0xFFFFEB3B), 10.0 to Color(0xFF43A047),
        5.0 to Color(0xFFFFFFFF), 2.5 to Color(0xFF9C27B0),
        1.25 to Color(0xFFFF9800), 1.0 to Color(0xFF00BCD4),
    )

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left plates (reversed for visual)
        plates.reversed().forEach { plate ->
            PlateDisc(weight = plate, color = plateColors[plate] ?: Color.Gray, unit = unit)
        }

        // Barbell bar
        Box(
            Modifier
                .width(80.dp)
                .height(12.dp)
                .background(Color(0xFF757575), RoundedCornerShape(6.dp))
        )

        // Right plates
        plates.forEach { plate ->
            PlateDisc(weight = plate, color = plateColors[plate] ?: Color.Gray, unit = unit)
        }
    }
}

@Composable
fun PlateDisc(weight: Double, color: Color, unit: String) {
    val height = when {
        weight >= 20 -> 56.dp
        weight >= 10 -> 48.dp
        weight >= 5 -> 40.dp
        else -> 32.dp
    }
    Box(
        Modifier
            .width(20.dp)
            .height(height)
            .background(color, RoundedCornerShape(3.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$weight",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = if (color == Color(0xFFFFFFFF)) Color.Black else Color.White
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
        while (remaining >= plate) {
            result.add(plate)
            remaining -= plate
        }
    }

    return result
}
```

### `core/ui/LoadingSkeleton.kt`
```kotlin
package com.example.gymlevels.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1200, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, height: Dp = 16.dp, cornerRadius: Dp = 8.dp) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush())
    )
}

@Composable
fun HomeScreenSkeleton() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonBox(Modifier.fillMaxWidth(), 100.dp, 16.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.weight(1f), 80.dp, 12.dp)
            SkeletonBox(Modifier.weight(1f), 80.dp, 12.dp)
            SkeletonBox(Modifier.weight(1f), 80.dp, 12.dp)
        }
        SkeletonBox(Modifier.fillMaxWidth().height(48.dp), 48.dp, 8.dp)
        repeat(4) {
            SkeletonBox(Modifier.fillMaxWidth(), 72.dp, 12.dp)
        }
    }
}

@Composable
fun ExerciseListSkeleton() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonBox(Modifier.fillMaxWidth(), 48.dp, 12.dp) // Search bar
        repeat(8) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                SkeletonBox(Modifier.size(40.dp), 40.dp, 8.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(0.7f), 16.dp)
                    SkeletonBox(Modifier.fillMaxWidth(0.5f), 12.dp)
                }
            }
        }
    }
}

@Composable
fun MuscleRankingsSkeleton() {
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonBox(Modifier.weight(1f).fillMaxHeight(), cornerRadius = 16.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(8) { SkeletonBox(Modifier.fillMaxWidth(), 32.dp, 8.dp) }
        }
    }
}
```

### `core/ui/EmptyState.kt`
```kotlin
package com.example.gymlevels.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("Something went wrong", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onRetry) { Text("Retry") }
        }
    }
}
```

### `core/ui/OfflineBanner.kt`
```kotlin
package com.example.gymlevels.core.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService

@Composable
fun OfflineBanner() {
    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val cm = context.getSystemService<ConnectivityManager>()!!
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOffline = false }
            override fun onLost(network: Network) { isOffline = true }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        // Check initial state
        val active = cm.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        isOffline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true

        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
            Text("No internet connection — working offline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
```

### `core/ui/XPGainToast.kt` (popup overlay used on WorkoutCompleteScreen and challenge completion)
```kotlin
@Composable
fun XPGainOverlay(xpAmount: Int, label: String = "XP", visible: Boolean, onDismiss: () -> Unit) {
    LaunchedEffect(visible) {
        if (visible) { delay(2000); onDismiss() }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { -it / 2 } + fadeOut(),
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Text(
                "+$xpAmount $label",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }
    }
}
```

### Integration into `MainActivity.kt` — wrap with OfflineBanner
```kotlin
setContent {
    GymLevelsTheme {
        Column(Modifier.fillMaxSize()) {
            OfflineBanner()
            GymLevelsNavGraph()
        }
    }
}
```

### Add Plate Calculator to NavGraph
```kotlin
// In NavGraph.kt — add route:
composable<Screen.PlateCalculator> {
    PlateCalculatorScreen(onBack = { navController.popBackStack() })
}
```

Add to `Screen.kt`:
```kotlin
@Serializable object PlateCalculator : Screen
```

Add to SettingsScreen → About section or accessible from ActiveWorkoutScreen as utility.

---

## Verification
1. PlateCalculatorScreen: enter 100kg → shows 2×20kg + 2×10kg per side diagram
2. Enter 142.5kg → shows correct plate combination
3. Switch to lbs → plate options change to 45/35/25/10/5/2.5
4. Loading skeleton: temporarily set 3s delay in HomeViewModel → HomeScreenSkeleton shows
5. Disconnect Wi-Fi → OfflineBanner slides down from top of screen
6. Reconnect → banner fades out
7. EmptyState: clear all workouts → WorkoutHistoryScreen shows EmptyState composable
8. ErrorState: mock Firestore failure → shows ErrorState with Retry button
