package com.hunterxdk.gymsololeveling.feature.achievements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.service.AchievementService
import com.hunterxdk.gymsololeveling.feature.profile.data.PlayerStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementService: AchievementService,
    private val playerStatsRepository: PlayerStatsRepository,
) : ViewModel() {

    val badges: StateFlow<List<AchievementService.BadgeProgress>> = playerStatsRepository
        .stats
        .map { stats -> achievementService.computeAllBadgeProgress(stats ?: return@map emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}