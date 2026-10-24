package com.example.micasaestucasa.ui.fragment.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.ChatRepository
import com.example.micasaestucasa.data.repository.StorageRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ModifyProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ModifyProfileUiState())
    val uiState: StateFlow<ModifyProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * Carica i dati attuali dell'utente dalla cache del Repository
     */
    private fun loadProfile() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val user = UsersRepository.currentUserProfile.value

            if (user != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = user.name,
                        surname = user.surname,
                        bio = user.bio,
                        profileImageUrl = user.profileImageUrl,
                        phoneNumber = user.phone
                    )
                }
            } else {
                val uid = UsersRepository.getCurrentUid()
                if (uid != null) {
                    UsersRepository.loadAndCacheProfile(uid).onSuccess {
                        loadProfile()
                    }.onFailure { e ->
                        handleError(e, "Errore nel caricamento del profilo")
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Utente non loggato") }
                }
            }
        }
    }

    fun updateName(newName: String) = _uiState.update { it.copy(name = newName) }
    fun updateSurname(newSurname: String) = _uiState.update { it.copy(surname = newSurname) }
    fun updateBio(newBio: String) = _uiState.update { it.copy(bio = newBio) }
    fun onImageSelected(uri: String) = _uiState.update { it.copy(newImageUri = uri) }
    fun updatePhoneNumber(newPhoneNumber: String) = _uiState.update { it.copy(phoneNumber = newPhoneNumber) }


    /**
     * Salva le modifiche su Firestore (e carica la foto se cambiata)
     */
    fun saveProfile() {
        val state = _uiState.value
        val currentUid = UsersRepository.getCurrentUid() ?: return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                var finalImageUrl = state.profileImageUrl

                state.newImageUri?.let { uri ->
                    val uploadResult = StorageRepository.uploadProfileImage(currentUid, uri)
                    finalImageUrl = uploadResult.getOrThrow()
                }

                val updates = mapOf(
                    "name" to state.name,
                    "surname" to state.surname,
                    "bio" to state.bio,
                    "profileImageUrl" to finalImageUrl,
                    "phone" to state.phoneNumber
                )
                UsersRepository.updateFields(currentUid, updates).getOrThrow()

                ChatRepository.syncUserImageDataInChats(currentUid, finalImageUrl)

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: Exception) {
                handleError(e, "Errore durante il salvataggio")
            }
        }
    }

    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi per modificare questo profilo."
        } else {
            "$context: ${e.localizedMessage ?: "Errore imprevisto"}"
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun resetSuccess() = _uiState.update { it.copy(isSuccess = false) }
}


