package com.hunterxdk.gymsololeveling.feature.onboarding.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass
import com.hunterxdk.gymsololeveling.ui.theme.GoldAccent
import kotlinx.coroutines.delay

val PlayerClass.classAssetName: String get() = when (this) {
    PlayerClass.STRENGTH_SEEKER      -> "strength_seeker"
    PlayerClass.MASS_BUILDER         -> "mass_builder"
    PlayerClass.ENDURANCE_HUNTER     -> "endurance_hunter"
    PlayerClass.BALANCE_WARRIOR      -> "balance_warrior"
    PlayerClass.RECOVERY_SPECIALIST  -> "recovery_specialist"
    PlayerClass.ATHLETE_ELITE        -> "athlete_elite"
}

@Composable
fun PlayerClassRevealScreen(
    playerClassName: String,
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val playerClass = remember {
        runCatching { enumValueOf<PlayerClass>(playerClassName) }
            .getOrDefault(PlayerClass.BALANCE_WARRIOR)
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800),
        label = "alpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "scale",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Your Class",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .clip(RoundedCornerShape(24.dp))
                .background(GoldAccent.copy(alpha = 0.1f))
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = "file:///android_asset/images/classes/${playerClass.classAssetName}.webp",
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    playerClass.displayName,
                    style = MaterialTheme.typography.displaySmall,
                    color = GoldAccent,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    playerClass.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = {
                viewModel.onRevealComplete()
                onContinue()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
        ) {
            Text(
                "Start Your Journey",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
            )
        }
    }
}

private fun PlayerClass.toEmoji(): String = when (this) {
    PlayerClass.STRENGTH_SEEKER -> "\uD83C\uDFCB"
    PlayerClass.MASS_BUILDER -> "\uD83D\uDCAA"
    PlayerClass.ENDURANCE_HUNTER -> "\uD83C\uDFC3"
    PlayerClass.RECOVERY_SPECIALIST -> "\uD83E\uDDE8"
    PlayerClass.ATHLETE_ELITE -> "\u26A1"
    PlayerClass.BALANCE_WARRIOR -> "\u2696\uFE0F"
}