package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.ReviewRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentUid = AuthRepository.getCurrentIUD()
            if (currentUid == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Errore: Utente non autenticato")
                }
                return@launch
            }

            try {
                val user = UsersRepository.currentUserProfile.value
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Errore: profilo non trovato") }
                    return@launch
                }

                if (!user.stato) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Errore: Utente disattivato")
                    }
                    return@launch
                }

                val userReviewsDef = async { ReviewRepository.getReviewsForUser(currentUid) }
                val userReviewsReceived = userReviewsDef.await().getOrThrow()

                val stats = UserStats(
                    ratingMedia = user.valutazioneMedia,
                    numeroRecensioni = user.numeroRecensioni,
                    numeroSoggiorni = user.numeroSoggiorni,
                    numeroAnnunci = user.numeroAnnunci
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        userStats = stats,
                        userReviewsReceived = userReviewsReceived,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Errore durante il caricamento del profilo", e)
                handleError(e, "Errore durante il caricamento del profilo")
            }
        }
    }

    /**
     * Gestione centralizzata degli errori per intercettare violazioni delle regole Firestore
     */
    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi necessari per visualizzare questi dati del profilo."
        } else {
            "$context: ${e.localizedMessage}"
        }
        _uiState.update {
            it.copy(isLoading = false, errorMessage = message)
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleReviews() {
        _uiState.update { it.copy(isShowingAllReviews = !it.isShowingAllReviews) }
    }

    fun logout() {
        AuthRepository.signOut()
        UsersRepository.clearCache()
        _uiState.update {
            it.copy(
                user = null,
                userStats = null,
                isLoggedOut = true
            )
        }
    }
}


