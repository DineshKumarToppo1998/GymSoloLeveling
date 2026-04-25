package com.hunterxdk.gymsololeveling.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.hunterxdk.gymsololeveling.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object GuestMode : AuthUiState
    data class Success(val user: FirebaseUser, val isNewUser: Boolean) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(idToken: String, isMigration: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    if (isMigration) {
                        authRepository.migrateGuestToAccount(user.uid)
                    }
                    val isOnboardingDone = authRepository.isOnboardingComplete()
                    _uiState.value = AuthUiState.Success(user, isNewUser = !isOnboardingDone && !isMigration)
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Google sign-in failed")
                }
        }
    }

    fun signInWithEmail(email: String, password: String, isMigration: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithEmail(email, password)
                .onSuccess { user ->
                    if (isMigration) {
                        authRepository.migrateGuestToAccount(user.uid)
                    }
                    val isOnboardingDone = authRepository.isOnboardingComplete()
                    _uiState.value = AuthUiState.Success(user, isNewUser = !isOnboardingDone && !isMigration)
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Sign-in failed")
                }
        }
    }

    fun createAccount(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.createAccountWithEmail(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user, isNewUser = true)
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Account creation failed")
                }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.continueAsGuest()
            _uiState.value = AuthUiState.GuestMode
        }
    }
}