package com.hunterxdk.gymsololeveling.feature.onboarding.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass
import com.hunterxdk.gymsololeveling.feature.onboarding.domain.OnboardingQuizData
import com.hunterxdk.gymsololeveling.feature.onboarding.domain.QuizOption
import com.hunterxdk.gymsololeveling.feature.onboarding.domain.QuizOptionType
import com.hunterxdk.gymsololeveling.feature.onboarding.domain.QuizStep
import com.hunterxdk.gymsololeveling.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingQuizScreen(
    onComplete: (playerClass: PlayerClass, answers: Map<Int, String>) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState { OnboardingQuizData.STEPS.size }
    val answers = remember { mutableStateMapOf<Int, String>() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1) / OnboardingQuizData.STEPS.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = GoldAccent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f),
        ) { page ->
            val step = OnboardingQuizData.STEPS[page]
            QuizStepPage(
                step = step,
                selectedIds = answers[page]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
                onSelect = { id ->
                    if (step.multiSelect) {
                        val current = answers[page]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
                        if (id == "none") {
                            answers[page] = "none"
                        } else {
                            current.remove("none")
                            if (!current.add(id)) current.remove(id)
                            answers[page] = current.joinToString(",")
                        }
                    } else {
                        answers[page] = id
                    }
                },
            )
        }

        val currentPage = pagerState.currentPage
        val hasAnswer = !answers[currentPage].isNullOrBlank()
        Button(
            onClick = {
                scope.launch {
                    if (currentPage < OnboardingQuizData.STEPS.size - 1) {
                        pagerState.animateScrollToPage(currentPage + 1)
                    } else {
                        val playerClass = com.hunterxdk.gymsololeveling.feature.onboarding.domain.PlayerClassAssigner.assign(answers.mapValues { it.value })
                        onComplete(playerClass, answers.toMap())
                    }
                }
            },
            enabled = hasAnswer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                disabledContainerColor = GoldAccent.copy(alpha = 0.3f)
            ),
        ) {
            Text(
                if (currentPage == OnboardingQuizData.STEPS.size - 1) "Reveal My Class" else "Next \u2192",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun QuizStepPage(
    step: QuizStep,
    selectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            step.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (step.subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                step.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(16.dp))

        val hasImages = step.options.any { it.type is QuizOptionType.ImageAsset }
        if (hasImages) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(step.options) { option ->
                    ImageOptionCard(
                        option = option,
                        selected = option.id in selectedIds,
                        onClick = { onSelect(option.id) },
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(step.options) { option ->
                    EmojiOptionCard(
                        option = option,
                        selected = option.id in selectedIds,
                        onClick = { onSelect(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageOptionCard(
    option: QuizOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val assetPath = (option.type as QuizOptionType.ImageAsset).assetPath
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(0.85f),
        border = if (selected) BorderStroke(2.dp, GoldAccent) else null,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box {
            AsyncImage(
                model = "file:///android_asset/$assetPath",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            Text(
                option.label,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun EmojiOptionCard(
    option: QuizOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val emoji = (option.type as QuizOptionType.Emoji).emoji
    Card(
        onClick = onClick,
        border = if (selected) BorderStroke(2.dp, GoldAccent) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) GoldAccent.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Text(
                option.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = GoldAccent)
        }
    }
}