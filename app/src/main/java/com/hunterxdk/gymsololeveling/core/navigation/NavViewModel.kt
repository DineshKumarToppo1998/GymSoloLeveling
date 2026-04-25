package com.hunterxdk.gymsololeveling.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import com.hunterxdk.gymsololeveling.core.domain.model.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    val sessionManager: SessionManager,
) : ViewModel() {
    val session: StateFlow<UserSession> = sessionManager.session
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSession.Loading)
}