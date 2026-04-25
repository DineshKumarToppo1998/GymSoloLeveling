package com.hunterxdk.gymsololeveling.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import com.hunterxdk.gymsololeveling.core.domain.model.PlayerStats
import com.hunterxdk.gymsololeveling.core.domain.model.User
import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass
import com.hunterxdk.gymsololeveling.feature.auth.data.AuthRepository
import com.hunterxdk.gymsololeveling.feature.profile.data.PlayerStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val playerStatsRepository: PlayerStatsRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val session: StateFlow<com.hunterxdk.gymsololeveling.core.domain.model.UserSession> = sessionManager.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.hunterxdk.gymsololeveling.core.domain.model.UserSession.Loading)

    init {
        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                playerStatsRepository.stats,
            ) { firebaseUser, stats ->
                ProfileUiState(
                    user = firebaseUser?.toDomainUser(stats),
                    stats = stats,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun signOut() {
        viewModelScope.launch { sessionManager.signOut() }
    }

    private fun FirebaseUser.toDomainUser(stats: PlayerStats?): User = User(
        uid = uid,
        email = email ?: "",
        displayName = displayName ?: "Warrior",
        photoUrl = photoUrl?.toString(),
        playerClass = stats?.playerClass ?: PlayerClass.BALANCE_WARRIOR,
        hasCompletedOnboarding = true,
    )
}

data class ProfileUiState(
    val user: User? = null,
    val stats: PlayerStats? = null,
    val isLoading: Boolean = false,
)