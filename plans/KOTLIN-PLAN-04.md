# KOTLIN-PLAN-04.md — Firebase Auth (Google + Email/Password)

## Goal
Implement full Firebase Authentication with Google Sign-In (Credential Manager API), Email/Password, AuthRepository, AuthViewModel, and SignUpScreen.

## Phase
Foundation — Phase 3 of 6. Depends on KOTLIN-PLAN-03.

---

## Files to Create

### `feature/auth/data/AuthRepositoryImpl.kt`
```kotlin
package com.example.gymlevels.feature.auth.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gymlevels.core.domain.model.User
import com.example.gymlevels.feature.auth.domain.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.toUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(activityContext: Context): Result<User> = runCatching {
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val result = credentialManager.getCredential(activityContext, request)
        val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user!!.toUser()
        ensureFirestoreProfile(user)
        user
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user!!.toUser()
    }

    override suspend fun createAccountWithEmail(email: String, password: String, displayName: String): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user!!.toUser().copy(displayName = displayName)
        ensureFirestoreProfile(user)
        user
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        firestore.collection("users").document(uid).delete().await()
        auth.currentUser!!.delete().await()
    }

    private suspend fun ensureFirestoreProfile(user: User) {
        val ref = firestore.collection("users").document(user.uid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "playerClass" to user.playerClass.name,
                "hasCompletedOnboarding" to false,
                "createdAt" to System.currentTimeMillis(),
            )).await()
        }
    }

    private fun com.google.firebase.auth.FirebaseUser.toUser() = User(
        uid = uid, email = email ?: "", displayName = displayName ?: "",
        photoUrl = photoUrl?.toString(),
    )

    companion object {
        // Replace with actual web client ID from google-services.json
        const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"
    }
}
```

### `feature/auth/domain/AuthRepository.kt`
```kotlin
interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signInWithGoogle(activityContext: Context): Result<User>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun createAccountWithEmail(email: String, password: String, displayName: String): Result<User>
    suspend fun signOut()
    suspend fun deleteAccount(): Result<Unit>
}
```

### `feature/auth/di/AuthModule.kt`
```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
```

### `feature/auth/presentation/AuthViewModel.kt`
```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefsDataStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithGoogle(activityContext)
                .onSuccess { user -> _uiState.update { it.copy(isLoading = false, signedInUser = user) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithEmail(email, password)
                .onSuccess { user -> _uiState.update { it.copy(isLoading = false, signedInUser = user) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun createAccount(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.createAccountWithEmail(email, password, displayName)
                .onSuccess { user -> _uiState.update { it.copy(isLoading = false, signedInUser = user, isNewUser = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val signedInUser: User? = null,
    val isNewUser: Boolean = false,
    val error: String? = null,
)
```

### `feature/auth/presentation/SignUpScreen.kt`
```kotlin
@Composable
fun SignUpScreen(
    onSignedIn: () -> Unit,
    onNewUser: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.signedInUser) {
        if (uiState.signedInUser != null) {
            if (uiState.isNewUser) onNewUser() else onSignedIn()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo / title
            Text(
                text = "GymLevels",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Level up your training",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(16.dp))

            if (isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            uiState.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (isSignUp) viewModel.createAccount(email, password, displayName)
                    else viewModel.signInWithEmail(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp))
                else Text(if (isSignUp) "Create Account" else "Sign In")
            }

            OutlinedButton(
                onClick = { viewModel.signInWithGoogle(context) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isLoading
            ) {
                Icon(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Continue with Google")
            }

            TextButton(onClick = { isSignUp = !isSignUp; viewModel.clearError() }) {
                Text(if (isSignUp) "Already have an account? Sign in" else "New here? Create account")
            }
        }
    }
}
```

---

## Prerequisite
- Place `google-services.json` (from Firebase console) in `app/` directory
- Update `WEB_CLIENT_ID` in `AuthRepositoryImpl` with the OAuth 2.0 web client ID from Firebase console
- Add `ic_google.xml` vector drawable to `res/drawable/`

---

## Verification
1. App launches → SignUpScreen visible
2. Email/password sign-up creates Firebase Auth user (visible in Firebase Console → Authentication)
3. Google Sign-In launches account picker, signs in successfully
4. After sign-in, navigation proceeds to Home (stub screen for now)
5. Cold restart with existing auth → starts on Home, not SignUpScreen
