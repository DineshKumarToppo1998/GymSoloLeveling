package com.hunterxdk.gymsololeveling.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import com.hunterxdk.gymsololeveling.core.domain.model.Challenge
import com.hunterxdk.gymsololeveling.core.domain.model.MuscleRank
import com.hunterxdk.gymsololeveling.core.domain.model.PlayerStats
import com.hunterxdk.gymsololeveling.core.domain.model.User
import com.hunterxdk.gymsololeveling.core.domain.model.UserSession
import com.hunterxdk.gymsololeveling.core.domain.model.effectiveUserId
import com.hunterxdk.gymsololeveling.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val prefsDataStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val session = sessionManager.session.first()
            val user: User? = when (session) {
                is UserSession.Authenticated -> User(
                    uid = session.uid,
                    email = session.email ?: "",
                    displayName = session.displayName ?: "Guest"
                )
                is UserSession.Guest -> User(
                    uid = session.localId,
                    email = "Guest",
                    displayName = "Guest User"
                )
                is UserSession.Loading -> null
            }

            _uiState.value = HomeUiState(
                user = user,
                currentStreak = 0,
                playerStats = PlayerStats(
                    userId = session.effectiveUserId,
                    totalWorkouts = 0,
                    totalXp = 0,
                    currentStreak = 0,
                    playerLevel = 1,
                ),
                muscleRanks = emptyList(),
                activeChallenges = emptyList(),
                isLoading = false,
            )
        }
    }

    fun onFilterSelected(filter: HomeFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}

data class HomeUiState(
    val user: User? = null,
    val currentStreak: Int = 0,
    val playerStats: PlayerStats? = null,
    val muscleRanks: List<MuscleRank> = emptyList(),
    val activeChallenges: List<Challenge> = emptyList(),
    val activeFilter: HomeFilter = HomeFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null,
)

enum class HomeFilter { ALL, OVERDUE, WEEKLY, MONTHLY }