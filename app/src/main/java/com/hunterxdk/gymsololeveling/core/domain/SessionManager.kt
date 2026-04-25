package com.hunterxdk.gymsololeveling.core.domain

import com.google.firebase.auth.FirebaseAuth
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import com.hunterxdk.gymsololeveling.core.domain.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val prefs: UserPreferencesDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<UserSession>(UserSession.Loading)
    val session: StateFlow<UserSession> = _session.asStateFlow()

    init {
        scope.launch { resolve() }
    }

    suspend fun resolve() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            _session.value = UserSession.Authenticated(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
            )
            return
        }
        val isGuest = prefs.isGuestMode.first()
        if (isGuest) {
            val localId = prefs.localUserId.first()
            _session.value = UserSession.Guest(localId)
            return
        }
        _session.value = UserSession.Loading
    }

    suspend fun setGuest() {
        prefs.enableGuestMode()
        val localId = prefs.localUserId.first()
        _session.value = UserSession.Guest(localId)
    }

    fun setAuthenticated(uid: String, email: String?, displayName: String?) {
        _session.value = UserSession.Authenticated(uid, email, displayName)
    }

    suspend fun signOut() {
        auth.signOut()
        prefs.clearGuestMode()
        _session.value = UserSession.Loading
    }

    val shouldSync: Boolean
        get() = _session.value is UserSession.Authenticated

    val currentUid: String?
        get() = (_session.value as? UserSession.Authenticated)?.uid
}