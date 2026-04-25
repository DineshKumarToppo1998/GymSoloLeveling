package com.hunterxdk.gymsololeveling.feature.challenges.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.model.Achievement
import com.hunterxdk.gymsololeveling.core.domain.model.Challenge
import com.hunterxdk.gymsololeveling.core.service.AchievementService
import com.hunterxdk.gymsololeveling.core.service.ChallengeService
import com.hunterxdk.gymsololeveling.feature.profile.data.PlayerStatsRepository
import com.hunterxdk.gymsololeveling.feature.rankings.data.MuscleRankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val challengeService: ChallengeService,
    private val achievementService: AchievementService,
    private val playerStatsRepository: PlayerStatsRepository,
    private val muscleRankRepository: MuscleRankRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengesUiState())
    val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadChallenges() }
    }

    private fun loadChallenges() {
        val challenges = challengeService.getDailyAndWeeklyChallenges()
        val daily = challenges.filter { it.id.startsWith("daily_") }
        val weekly = challenges.filter { it.id.startsWith("weekly_") }
        _uiState.update { it.copy(dailyChallenges = daily, weeklyChallenges = weekly) }
    }
}

data class ChallengesUiState(
    val dailyChallenges: List<Challenge> = emptyList(),
    val weeklyChallenges: List<Challenge> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val recentlyEarned: List<Achievement> = emptyList(),
    val isLoading: Boolean = false,
)
