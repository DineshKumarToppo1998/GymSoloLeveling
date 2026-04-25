package com.hunterxdk.gymsololeveling.feature.profile.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hunterxdk.gymsololeveling.core.data.preferences.UserPreferencesDataStore
import com.hunterxdk.gymsololeveling.core.domain.model.enums.PlayerClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class EditProfileUiState(
    val displayName: String = "",
    val photoUrl: String? = null,
    val photoUri: Uri? = null,
    val unit: String = "kg",
    val playerClass: PlayerClass? = null,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val user = auth.currentUser
            val unit = prefs.preferredUnit.first()
            val classStr = prefs.playerClass.first()
            _uiState.value = EditProfileUiState(
                displayName = user?.displayName ?: "",
                photoUrl = user?.photoUrl?.toString(),
                unit = unit,
                playerClass = classStr.takeIf { it.isNotEmpty() }?.let { runCatching { enumValueOf<PlayerClass>(it) }.getOrNull() },
            )
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(displayName = name) }
    fun onUnitChange(unit: String) = _uiState.update { it.copy(unit = unit) }
    fun onPhotoSelected(uri: Uri) = _uiState.update { it.copy(photoUri = uri) }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            prefs.setUnit(state.unit)

            val user = auth.currentUser ?: return@launch
            val updates = UserProfileChangeRequest.Builder()
                .setDisplayName(state.displayName)

            if (state.photoUri != null) {
                val ref = FirebaseStorage.getInstance().reference.child("avatars/${user.uid}.jpg")
                ref.putFile(state.photoUri).await()
                val downloadUrl = ref.downloadUrl.await()
                updates.setPhotoUri(downloadUrl)
            }

            user.updateProfile(updates.build()).await()
            firestore.collection("users").document(user.uid)
                .update(mapOf("displayName" to state.displayName)).await()
        }
    }
}