# KOTLIN-PLAN-04-01.md — Guest Mode & Local-First Architecture

## Goal
Add "Continue without account" guest mode. All data stored locally in Room. When guest later
signs in, local progress is migrated to Firestore. Auth is now optional, not mandatory.

## Depends On
KOTLIN-PLAN-04 (AuthRepository, AuthViewModel, SignUpScreen)

## Must Complete Before
KOTLIN-PLAN-05 (Onboarding — needs to know if user is guest or authenticated)

---

## Concept

```
App launch
  └─ Firebase.auth.currentUser != null  ──► Home (authenticated)
  └─ DataStore guestModeEnabled == true ──► Home (guest)
  └─ else                               ──► SignUpScreen

SignUpScreen
  ├─ Continue with Google
  ├─ Sign In / Create Account (email)
  └─ [NEW] "Continue without account →"

Guest user:
  - All Room writes happen as normal
  - All Firestore writes are SKIPPED (no-op)
  - Profile stored in DataStore + Room only
  - No uid — use a stable local UUID stored in DataStore

Sign-in from guest:
  - AuthRepository.migrateGuestToAccount(uid) runs
  - Reads all Room data → bulk-writes to Firestore
  - Clears guestModeEnabled flag
  - User is now authenticated, data is synced
```

---

## Files to Modify

### 1. `core/data/preferences/UserPreferencesDataStore.kt`
Add two new keys:

```kotlin
val isGuestMode: Flow<Boolean> = store.data.map { it[Keys.GUEST_MODE] ?: false }
val localUserId: Flow<String> = store.data.map {
    it[Keys.LOCAL_USER_ID] ?: ""
}

suspend fun enableGuestMode() {
    store.edit {
        it[Keys.GUEST_MODE] = true
        // Generate stable local UUID if not already set
        if (it[Keys.LOCAL_USER_ID].isNullOrEmpty()) {
            it[Keys.LOCAL_USER_ID] = java.util.UUID.randomUUID().toString()
        }
    }
}

suspend fun clearGuestMode() = store.edit {
    it[Keys.GUEST_MODE] = false
}

// In Keys object:
val GUEST_MODE = booleanPreferencesKey("guest_mode")
val LOCAL_USER_ID = stringPreferencesKey("local_user_id")
```

---

### 2. `core/domain/model/UserSession.kt` — NEW FILE

```kotlin
package com.hunterxdk.gymsololeveling.core.domain.model

sealed interface UserSession {
    /** Firebase authenticated user */
    data class Authenticated(val uid: String, val email: String?, val displayName: String?) : UserSession

    /** Local-only guest — no Firebase uid */
    data class Guest(val localId: String) : UserSession

    /** Not yet determined (startup) */
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
```

---

### 3. `core/domain/SessionManager.kt` — NEW FILE

Single source of truth for who the current user is. Injected everywhere instead of
directly using `FirebaseAuth`.

```kotlin
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
        _session.value = UserSession.Loading // unauthenticated, show sign-in
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
}
```

---

### 4. `feature/auth/data/AuthRepository.kt` — MODIFY

Add guest sign-in and migration:

```kotlin
// Add to existing AuthRepository:

suspend fun continueAsGuest() {
    sessionManager.setGuest()
}

/**
 * Called after a guest user signs in or creates an account.
 * Reads all local Room data and bulk-writes it to Firestore.
 */
suspend fun migrateGuestToAccount(
    uid: String,
    muscleRankDao: MuscleRankDao,
    activeWorkoutDao: ActiveWorkoutDao,
): Result<Unit> = runCatching {
    // 1. Migrate muscle ranks
    val ranks = muscleRankDao.getAllOnce() // add this suspend query to MuscleRankDao
    if (ranks.isNotEmpty()) {
        val batch = firestore.batch()
        ranks.forEach { rank ->
            val ref = firestore
                .collection("users").document(uid)
                .collection("muscleRanks").document(rank.muscleGroup)
            batch.set(ref, rank.toFirestoreMap())
        }
        batch.commit().await()
    }

    // 2. Update Firestore user doc
    createUserDocIfNeeded(auth.currentUser!!)

    // 3. Clear guest mode flag
    prefs.clearGuestMode()
    sessionManager.setAuthenticated(uid, auth.currentUser?.email, auth.currentUser?.displayName)
}
```

Also inject `SessionManager` into `AuthRepository`:
```kotlin
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: UserPreferencesDataStore,
    private val sessionManager: SessionManager,  // ADD
)
```

---

### 5. `feature/auth/ui/AuthViewModel.kt` — MODIFY

Add `continueAsGuest()`:

```kotlin
fun continueAsGuest() {
    viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        authRepository.continueAsGuest()
        _uiState.value = AuthUiState.GuestMode
    }
}
```

Add `GuestMode` to `AuthUiState`:
```kotlin
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object GuestMode : AuthUiState           // ADD
    data class Success(val user: FirebaseUser, val isNewUser: Boolean) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
```

---

### 6. `feature/auth/ui/SignUpScreen.kt` — MODIFY

Add guest button below the email section:

```kotlin
// After the EmailAuthSection composable in SignUpScreen body:
Spacer(Modifier.height(16.dp))

TextButton(
    onClick = { viewModel.continueAsGuest() },
    modifier = Modifier.fillMaxWidth(),
    enabled = uiState !is AuthUiState.Loading,
) {
    Text(
        text = "Continue without account →",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        style = MaterialTheme.typography.bodyMedium,
    )
}
```

Add `GuestMode` handling in `LaunchedEffect(uiState)`:
```kotlin
is AuthUiState.GuestMode -> {
    onNavigateToOnboarding()   // guest always goes through onboarding
    viewModel.resetState()
}
```

---

### 7. `core/navigation/NavGraph.kt` — MODIFY

Update start destination logic to check `SessionManager`:

```kotlin
@Composable
fun GymLevelsNavGraph(sessionManager: SessionManager = hiltViewModel<NavViewModel>().sessionManager) {
    val session by sessionManager.session.collectAsState()
    val navController = rememberNavController()

    val startDestination: Screen = when (session) {
        is UserSession.Authenticated -> Screen.Home
        is UserSession.Guest -> Screen.Home
        is UserSession.Loading -> Screen.SignUp
    }
    // rest unchanged...
}
```

Or simpler — keep existing Firebase check, add guest check:
```kotlin
val startDestination: Screen = when {
    Firebase.auth.currentUser != null -> Screen.Home
    // Read DataStore synchronously via runBlocking only at startup (acceptable for splash)
    else -> Screen.SignUp  // SessionManager.resolve() handles the rest via LaunchedEffect
}
```

Add a `LaunchedEffect` in `NavGraph` that observes `sessionManager.session` and navigates
reactively when session changes (guest logs in → Home, signed-out → SignUp).

---

### 8. All repositories — ADD sync guard

Every repository method that writes to Firestore must check session first:

```kotlin
// Pattern to use in WorkoutRepository, ExerciseRepository, etc.:
private suspend fun shouldSync(): Boolean =
    sessionManager.session.value.isAuthenticated

// Example in WorkoutRepository.completeWorkout():
if (shouldSync()) {
    firestore.collection("users").document(uid).collection("workouts")
        .document(session.id).set(workoutMap).await()
}
// Room write always happens regardless of auth state
```

---

### 9. `feature/profile/ui/ProfileScreen.kt` — ADD sync banner (future, note for PLAN-12)

When `session.isGuest`, show a banner at the top of ProfileScreen:
```
"Your data is saved locally only.  [Sign in to sync →]"
```
Tapping "Sign in to sync" → navigate to SignUpScreen with a flag indicating
it's a migration flow (so on success, `migrateGuestToAccount()` runs).

---

## Data Flow Summary

```
Guest user logs a workout:
  WorkoutPersistenceManager → Room ✅
  Firestore write → SKIPPED ✅

Guest user signs in:
  AuthRepository.migrateGuestToAccount() runs
  Room muscle ranks → Firestore batch write ✅
  Room workouts → Firestore batch write ✅ (add to migration)
  prefs.clearGuestMode() ✅
  SessionManager.session emits Authenticated ✅
  NavGraph reacts → stays on Home ✅

Returning authenticated user:
  Firebase.auth.currentUser != null → Home, all writes go to Firestore ✅
```

---

## Room DAOs — Additional Queries Needed

Add to `MuscleRankDao.kt`:
```kotlin
@Query("SELECT * FROM muscle_ranks")
suspend fun getAllOnce(): List<MuscleRankEntity>  // non-Flow, for migration
```

---

## Verification

1. Fresh install → SignUpScreen shows "Continue without account →" at bottom
2. Tap it → goes to OnboardingQuiz (same as new authenticated user)
3. Log a workout as guest → Room has the workout, Firestore has nothing
4. Go to Settings → Sign In → complete sign-in → `migrateGuestToAccount()` runs →
   check Firestore → muscle ranks document exists under `users/{uid}/muscleRanks/`
5. Reinstall app → sign in with same account → progress is restored from Firestore

---

## Execution Order Within This Plan

1. `UserSession.kt` + `SessionManager.kt` (no dependencies)
2. `UserPreferencesDataStore.kt` additions
3. `AuthRepository.kt` + `AuthViewModel.kt` additions
4. `SignUpScreen.kt` guest button
5. Repository sync guards (skeleton — full implementation as each repo is built in later plans)
6. `NavGraph.kt` session-reactive navigation
