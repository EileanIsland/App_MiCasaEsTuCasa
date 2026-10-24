package com.example.micasaestucasa.ui.fragment.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.User
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.ChatRepository
import com.example.micasaestucasa.data.repository.ReviewRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import com.example.micasaestucasa.data.model.UserStats
import com.example.micasaestucasa.utils.DateUtils.formatDate
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()


    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            UsersRepository.currentUserProfile.collect { user ->
                if (user != null) {
                    _uiState.update { it.copy(
                        user = user,
                        isLoading = false,
                    )}
                }
            }
        }
    }

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
                var user = UsersRepository.currentUserProfile.value
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Errore: profilo non trovato") }
                    return@launch
                }

                if (user.accountStatus == "blocked") {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Il tuo account è stato bloccato permanentemente."
                        )
                    }
                    logout()
                    return@launch
                }

                if (user.accountStatus == "suspended") {
                    val isNowActive = activateUser(user)
                    if (isNowActive) {
                        user = UsersRepository.currentUserProfile.value!!
                    } else {
                        return@launch
                    }
                }

                val isAdmin = user.ruolo == "admin"

                if (isAdmin) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            isAdmin = true,
                            userStats = null,
                            userReviewsReceived = emptyList()
                        )
                    }
                }else {
                    val userReviewsDef = async { ReviewRepository.getReviewsForUser(currentUid) }
                    val userReviewsReceived = userReviewsDef.await().getOrDefault(emptyList())

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
                            isAdmin = false,
                        )
                    }
                }

            } catch (e: Exception) {
                handleError(e, "Errore durante il caricamento del profilo")
            }
        }
    }


    private suspend fun activateUser(user: User): Boolean{
        val now = System.currentTimeMillis()
        val expiry = user.suspendedUntil

        if(expiry < now){
            val modifiche = mapOf(
                "accountStatus" to "active",
                "suspendedUntil" to 0L
            )
            val result = UsersRepository.updateFields(user.id, modifiche)
            return if (result.isSuccess) true
            else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Errore durante la riattivazione") }
                false
            }
        } else {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Account disattivato. Scadenza: ${formatDate(expiry)}")
            }
            return false
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
        ChatRepository.stopGlobalChatListener()
        BookingRepository.stopBookingListener()

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


