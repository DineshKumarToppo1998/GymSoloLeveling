package com.hunterxdk.gymsololeveling.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import com.hunterxdk.gymsololeveling.core.domain.model.UserSession
import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass
import com.hunterxdk.gymsololeveling.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefsDataStore: UserPreferencesDataStore,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    fun completeQuiz(playerClass: PlayerClass, answers: Map<Int, String>) {
        viewModelScope.launch {
            prefsDataStore.setPlayerClass(playerClass.name)
            answers[4]?.let { prefsDataStore.setAvailableEquipment(it) }
            answers[6]?.let { prefsDataStore.setInjuries(it) }
            prefsDataStore.setOnboardingDone()

            val session = sessionManager.session.first()
            when (session) {
                is UserSession.Authenticated -> {
                    val uid = session.uid
                    firestore.collection("users").document(uid).update(mapOf(
                        "playerClass" to playerClass.name,
                        "hasCompletedOnboarding" to true,
                    )).await()
                }
                is UserSession.Guest -> { }
                is UserSession.Loading -> return@launch
            }
        }
    }

    fun onRevealComplete() {
        _isComplete.value = true
    }
}