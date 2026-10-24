package com.example.micasaestucasa.ui.fragment.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.ReviewRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import com.example.micasaestucasa.data.model.UserStats
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    /**
     * Carica il profilo di un altro utente (Guest o Host).
     * Utilizza async per caricare recensioni, soggiorni e annunci in parallelo.
     */
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userDef = async { UsersRepository.getUserById(userId) }
                val userReviewsDef = async { ReviewRepository.getReviewsForUser(userId) }

                val user = userDef.await().getOrThrow()
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Utente non trovato") }
                    return@launch
                }

                val userReviews = userReviewsDef.await().getOrThrow()
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
                        reviews = userReviews,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                handleError(e, "Errore durante il caricamento del profilo")
            }
        }
    }

    /**
     * Segnala un utente.
     */
    fun reportUser(reason: String) {
        val targetUserId = _uiState.value.user?.id ?: return
        val currentUser = UsersRepository.currentUserProfile.value ?: return

        viewModelScope.launch {
            UsersRepository.reportUser(targetUserId, currentUser.name, reason)
                .onSuccess {
                    _uiState.update { it.copy(feedback = "Utente segnalato con successo") }
                }
                .onFailure { e ->
                    handleError(e, "Errore durante la segnalazione dell'utente")
                }
        }
    }

    /**
     * Gestione centralizzata degli errori per catturare Permission Denied.
     */
    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi per visualizzare i dettagli di questo profilo."
        } else {
            "$context: ${e.localizedMessage}"
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun toggleReviews() {
        _uiState.update { it.copy(isShowingAllReviews = !it.isShowingAllReviews) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedback = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}






