package com.hunterxdk.gymsololeveling.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hunterxdk.gymsololeveling.core.data.local.dao.MuscleRankDao
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: UserPreferencesDataStore,
    private val sessionManager: SessionManager,
    private val muscleRankDao: MuscleRankDao,
) {
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        createUserDocIfNeeded(result.user!!)
        result.user!!
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user!!
    }

    suspend fun createAccountWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        createUserDocIfNeeded(result.user!!)
        result.user!!
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun isOnboardingComplete(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getBoolean("hasCompletedOnboarding") == true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun createUserDocIfNeeded(user: FirebaseUser) {
        val ref = firestore.collection("users").document(user.uid)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            ref.set(
                mapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "displayName" to (user.displayName ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "hasCompletedOnboarding" to false,
                )
            ).await()
        }
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No user signed in")
        val uid = user.uid
        firestore.collection("users").document(uid).delete().await()
        user.delete().await()
        auth.signOut()
    }

    suspend fun continueAsGuest() {
        sessionManager.setGuest()
    }

    suspend fun migrateGuestToAccount(uid: String): Result<Unit> = runCatching {
        val ranks = muscleRankDao.getAll()
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
        createUserDocIfNeeded(auth.currentUser!!)
        prefs.clearGuestMode()
        sessionManager.setAuthenticated(uid, auth.currentUser?.email, auth.currentUser?.displayName)
    }
}
