# KOTLIN-PLAN-22.md — Guest Mode Complete Wiring + Firestore Sync Guards

## Goal
Complete the guest mode implementation started in PLAN-04-01:
1. NavGraph reacts to `SessionManager.session` changes (no more direct Firebase.auth check)
2. Every repository that writes to Firestore has a `shouldSync()` guard
3. Verify all Firestore write paths exist in WorkoutRepository and MuscleRankRepository
4. Sign-in from guest triggers `migrateGuestToAccount()`
5. Profile screen shows guest banner with "Sign in to sync →"

## Depends On
PLAN-04 (AuthRepository, SessionManager), PLAN-09 (WorkoutRepository, MuscleRankRepository)

---

## 1. NavGraph — Reactive Session

### File: `core/navigation/NavGraph.kt` — MODIFY

Remove the static `Firebase.auth.currentUser` check. Use `SessionManager` via `NavViewModel`.

```kotlin
@Composable
fun GymLevelsNavGraph(
    navViewModel: NavViewModel = hiltViewModel(),
) {
    val session by navViewModel.session.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Reactive: if session changes to Loading → pop to SignUp
    LaunchedEffect(session) {
        when (session) {
            is UserSession.Loading -> {
                navController.navigate(Screen.SignUp) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> { /* handled by startDestination on first launch */ }
        }
    }

    val startDestination: Screen = when (session) {
        is UserSession.Authenticated, is UserSession.Guest -> Screen.Home
        is UserSession.Loading -> Screen.SignUp
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // ... all composable routes unchanged ...
    }
}
```

### File: `core/navigation/NavViewModel.kt` — MODIFY

Expose session from SessionManager:

```kotlin
@HiltViewModel
class NavViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {
    val session: StateFlow<UserSession> = sessionManager.session
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSession.Loading)
}
```

---

## 2. `shouldSync()` Pattern — Add to All Repos

Every repository that touches Firestore must check session before writing.
Room writes always happen regardless.

### Add helper to `SessionManager`:
```kotlin
val shouldSync: Boolean
    get() = session.value is UserSession.Authenticated

val currentUid: String?
    get() = (session.value as? UserSession.Authenticated)?.uid
```

### `feature/workout/data/WorkoutRepository.kt` — VERIFY / ADD guard

```kotlin
// Every Firestore write block should be wrapped like this:
suspend fun saveCompletedWorkout(session: WorkoutSession) {
    // Always write to Room
    val entity = session.toEntity()
    workoutDao.insertCompletedWorkout(entity)

    // Only sync to Firestore if authenticated
    if (sessionManager.shouldSync) {
        val uid = sessionManager.currentUid ?: return
        firestore.collection("users").document(uid)
            .collection("workouts").document(session.id)
            .set(session.toFirestoreMap()).await()
    }
}
```

### `feature/rankings/data/MuscleRankRepository.kt` — VERIFY / ADD guard

```kotlin
suspend fun updateMuscleXP(updates: Map<String, Int>) {
    val uid = sessionManager.currentUid
    updates.forEach { (muscle, xp) ->
        // Always update Room
        muscleRankDao.addXP(muscle, xp)

        // Firestore only if authenticated
        if (sessionManager.shouldSync && uid != null) {
            firestore.collection("users").document(uid)
                .collection("muscleRanks").document(muscle)
                .set(mapOf("xp" to xp, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
        }
    }
}
```

### Pattern for all other repos (apply same):
- `WorkoutHistoryRepository` — history reads from Room, no Firestore guard needed (reads only)
- `PlayerStatsRepository` — reads from Room + local computation, no write guard needed
- `WeightRepository` (PLAN-20) — add guard on Firestore write if added later

---

## 3. MuscleRankDao — Add Missing Query

PLAN-04-01 specifies `getAllOnce()` for migration. Add if missing:

```kotlin
// In MuscleRankDao.kt
@Query("SELECT * FROM muscle_ranks")
suspend fun getAllOnce(): List<MuscleRankEntity>
```

---

## 4. Guest → Sign-In Migration Flow

When a guest taps "Sign in to sync" from ProfileScreen:

### `feature/auth/ui/SignUpScreen.kt` — ADD `isMigration: Boolean` param

```kotlin
@Composable
fun SignUpScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    isMigration: Boolean = false,    // ADD
    viewModel: AuthViewModel = hiltViewModel(),
) {
    // In LaunchedEffect(uiState):
    is AuthUiState.Success -> {
        if (isMigration) {
            // Guest data already in Room; migration runs in AuthViewModel
            onNavigateToHome()
        } else if (state.isNewUser) {
            onNavigateToOnboarding()
        } else {
            onNavigateToHome()
        }
        viewModel.resetState()
    }
}
```

### `feature/auth/ui/AuthViewModel.kt` — Trigger migration on guest sign-in

```kotlin
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
            .onFailure { e -> _uiState.value = AuthUiState.Error(e.message ?: "Sign-in failed") }
    }
}

// Same for signInWithEmail and createAccount
```

### `Screen.kt` — Add migration flag to SignUp route:
```kotlin
@Serializable data class SignUp(val isMigration: Boolean = false) : Screen
```

### NavGraph wiring for migration:
```kotlin
composable<Screen.SignUp> { backStackEntry ->
    val args = backStackEntry.toRoute<Screen.SignUp>()
    SignUpScreen(
        isMigration = args.isMigration,
        onNavigateToOnboarding = { navController.navigate(Screen.OnboardingQuiz) { popUpTo(Screen.SignUp) { inclusive = true } } },
        onNavigateToHome = { navController.navigate(Screen.Home) { popUpTo(Screen.SignUp) { inclusive = true } } },
    )
}
```

---

## 5. ProfileScreen — Guest Sync Banner

### `feature/profile/presentation/ProfileScreen.kt` — ADD

```kotlin
// Inject sessionManager via ProfileViewModel
val session by viewModel.session.collectAsStateWithLifecycle()

// At top of profile screen content:
if (session.isGuest) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CloudOff, null, tint = GoldAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Data saved locally only.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                navController.navigate(Screen.SignUp(isMigration = true))
            }) {
                Text("Sign in to sync →", color = GoldAccent, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
```

### `feature/profile/presentation/ProfileViewModel.kt` — ADD:
```kotlin
val session: StateFlow<UserSession> = sessionManager.session
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSession.Loading)
```

---

## 6. Sign Out Wiring

Ensure Settings sign-out goes through `SessionManager.signOut()` not `FirebaseAuth` directly:

```kotlin
// In SettingsViewModel:
fun signOut() {
    viewModelScope.launch {
        sessionManager.signOut()   // clears Firebase + DataStore guestMode flag
        // NavGraph LaunchedEffect(session) reacts → navigates to SignUp
    }
}
```

---

## Execution Order

1. `NavViewModel` expose `session` StateFlow
2. `NavGraph` switch to session-reactive `startDestination` + `LaunchedEffect`
3. `SessionManager.shouldSync` + `currentUid` helpers
4. `WorkoutRepository` — add/verify Firestore guard
5. `MuscleRankRepository` — add/verify Firestore guard
6. `MuscleRankDao.getAllOnce()` — add if missing
7. `Screen.SignUp(isMigration)` param + `AuthViewModel` migration hooks
8. `SignUpScreen` `isMigration` param handling
9. NavGraph SignUp wiring with `isMigration`
10. `ProfileScreen` + `ProfileViewModel` — guest banner + session flow
11. `SettingsViewModel.signOut()` → SessionManager
12. `./gradlew assembleDebug`

## Verification

1. Fresh install → no account → app lands on SignUp ✅
2. Tap "Continue without account" → lands on OnboardingQuiz ✅
3. Complete onboarding as guest → Home screen works, data saves to Room ✅
4. Log a workout as guest → Room has workout, Firestore has NO new document ✅
5. Profile → "Sign in to sync →" banner visible ✅
6. Tap banner → SignUp screen (migration mode) → sign in with Google → Room data migrated to Firestore ✅
7. Sign out → app returns to SignUp ✅
8. Sign in again → same account → data restored from Firestore ✅
