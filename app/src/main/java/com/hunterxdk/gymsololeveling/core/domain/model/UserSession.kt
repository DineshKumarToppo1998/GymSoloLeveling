package com.hunterxdk.gymsololeveling.core.domain.model

sealed interface UserSession {
    data class Authenticated(val uid: String, val email: String?, val displayName: String?) : UserSession
    data class Guest(val localId: String) : UserSession
    data object Loading : UserSession
}

val UserSession.effectiveUserId: String
    get() = when (this) {
        is UserSession.Authenticated -> uid
        is UserSession.Guest -> localId
        is UserSession.Loading -> ""
    }

val UserSession.isAuthenticated: Boolean
    get() = this is UserSession.Authenticated

val UserSession.isGuest: Boolean
    get() = this is UserSession.Guest