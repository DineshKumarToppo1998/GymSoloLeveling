package com.hunterxdk.gymsololeveling.feature.rankings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunterxdk.gymsololeveling.core.domain.model.MuscleRank
import com.hunterxdk.gymsololeveling.core.domain.model.XPThresholds
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankTier
import com.hunterxdk.gymsololeveling.core.service.RankService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleRankingsScreen(
    onBack: () -> Unit,
    viewModel: MuscleRankingsViewModel = hiltViewModel(),
) {
    val ranks by viewModel.ranks.collectAsStateWithLifecycle()
    val selectedMuscle by viewModel.selectedMuscle.collectAsStateWithLifecycle()
    var isFrontView by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Muscle Rankings") },
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
                .padding(16.dp),
        ) {
            MuscleViewToggle(
                isFrontView = isFrontView,
                onToggle = { isFrontView = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MuscleBodyView(
                    muscleRanks = ranks,
                    isFrontView = isFrontView,
                    onMuscleSelected = { muscle ->
                        ranks.firstOrNull { it.muscleGroup == muscle }
                            ?.let { viewModel.onMuscleSelected(it) }
                    },
                    selectedMuscle = selectedMuscle?.muscleGroup,
                    modifier = Modifier.weight(1f),
                )

                val visibleMuscles = if (isFrontView) {
                    listOf(
                        MuscleGroup.CHEST,
                        MuscleGroup.FRONT_SHOULDERS,
                        MuscleGroup.BICEPS,
                        MuscleGroup.ABS,
                        MuscleGroup.OBLIQUES,
                        MuscleGroup.QUADRICEPS,
                        MuscleGroup.CALVES,
                    )
                } else {
                    listOf(
                        MuscleGroup.BACK_SHOULDERS,
                        MuscleGroup.TRAPS,
                        MuscleGroup.UPPER_BACK,
                        MuscleGroup.LATS,
                        MuscleGroup.MIDDLE_BACK,
                        MuscleGroup.LOWER_BACK,
                        MuscleGroup.GLUTES,
                        MuscleGroup.HAMSTRINGS,
                    )
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(visibleMuscles) { muscleGroup ->
                        val rank = ranks.firstOrNull { it.muscleGroup == muscleGroup }
                        MuscleRankRow(
                            muscleGroup = muscleGroup,
                            rank = rank,
                            isSelected = selectedMuscle?.muscleGroup == muscleGroup,
                            onClick = { rank?.let { viewModel.onMuscleSelected(it) } },
                        )
                    }
                }
            }

            AnimatedVisibility(visible = selectedMuscle != null) {
                selectedMuscle?.let { rank ->
                    Column {
                        Spacer(Modifier.height(16.dp))
                        MuscleDetailCard(rank = rank)
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleViewToggle(
    isFrontView: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf("Front", "Back")
    val selectedIndex = if (isFrontView) 0 else 1

    Row(
        modifier = modifier
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onToggle(index == 0) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleRankRow(
    muscleGroup: MuscleGroup,
    rank: MuscleRank?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val rankTier = rank?.currentRank ?: RankTier.UNTRAINED
    val rankColor = RankService.getRankColor(rankTier)

    Surface(
        onClick = onClick,
        color = if (isSelected) rankColor.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(rankColor, CircleShape),
            )
            Text(
                text = muscleGroup.displayName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = RankService.getRankDisplayName(rankTier, rank?.currentSubRank),
                style = MaterialTheme.typography.labelSmall,
                color = rankColor,
            )
        }
    }
}

@Composable
private fun MuscleDetailCard(rank: MuscleRank) {
    val rankColor = RankService.getRankColor(rank.currentRank)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = rank.muscleGroup.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = RankService.getRankDisplayName(rank.currentRank, rank.currentSubRank),
                    style = MaterialTheme.typography.titleSmall,
                    color = rankColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = "${rank.totalXp} XP total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            if (rank.currentRank != RankTier.LEGEND) {
                val progress = remember(rank.totalXp) {
                    val thresholds = XPThresholds.SUB_RANK_THRESHOLDS
                    val nextThreshold = thresholds.firstOrNull { it.third > rank.totalXp }?.third?.toFloat()
                        ?: (rank.totalXp.toFloat() + 1f)
                    val prevThreshold = thresholds.lastOrNull { it.third <= rank.totalXp }?.third?.toFloat()
                        ?: 0f
                    ((rank.totalXp - prevThreshold) / (nextThreshold - prevThreshold)).coerceIn(0f, 1f)
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = rankColor,
                )
                Text(
                    text = "${rank.xpToNextRank} XP to next rank",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            } else {
                Text(
                    text = "Maximum rank achieved!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = rankColor,
                )
            }
        }
    }
}
