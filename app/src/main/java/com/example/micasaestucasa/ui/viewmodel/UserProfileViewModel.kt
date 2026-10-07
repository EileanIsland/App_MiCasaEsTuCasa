package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.ReviewTarget
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.ReviewRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState : StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile(userId: String){
        viewModelScope.launch{
            _uiState.update{it.copy(isLoading = true, errorMessage = null)}

            try{
                val userDef = async { UsersRepository.getUserById(userId)}
                val userReviewsDef = async { ReviewRepository.getReviewsForUser(userId)}
                val staysDef = async { BookingRepository.getBookingByGuest(userId)}
                val adsDef = async { BookingRepository.getBookingByHost(userId)}


                val user = userDef.await().getOrThrow()
                val userReviews = userReviewsDef.await().getOrThrow()
                val (media, totale) = ReviewRepository.getRatingStats(user?.id ?: "", ReviewTarget.UTENTE).getOrThrow()
                val stays = staysDef.await().getOrThrow()
                val ads = adsDef.await().getOrThrow()

                val stats = UserStats(
                    ratingMedia = media,
                    numeroRecensioni = totale,
                    numeroSoggiorni = stays.size,
                    numeroAnnunci = ads.size

                )

                _uiState.update{
                    it.copy(
                        isLoading = false,
                        user = user,
                        userStats = stats,
                        reviews = userReviews,
                        errorMessage = if (user == null) "Utente non trovato" else null
                    )
                }

            }catch (e: Exception){
                _uiState.update{
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore durante il caricamento del profilo: ${e.message}"
                    )
                }
            }
        }

    }

    fun toggleReviews(){
        _uiState.update { it.copy(isShowingAllReviews = !it.isShowingAllReviews) }
    }


    fun reportUser(reason: String) {
        viewModelScope.launch {
            val state = _uiState.value
            UsersRepository.reportUser(state.user?.id ?: "", reason).onSuccess {
                _uiState.update { it.copy(
                    feedback = "Utente segnalato con successo"
                ) }
            }.onFailure {
                _uiState.update { it.copy(
                    errorMessage = "Errore durante segnalazione dell'utente"
                ) }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedback = null) }
    }


    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

}