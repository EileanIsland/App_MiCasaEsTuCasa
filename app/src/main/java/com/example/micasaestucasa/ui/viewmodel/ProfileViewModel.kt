package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.ReviewTarget
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.BookingRepository
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

    fun loadProfile(){
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentUid = AuthRepository.getCurrentIUD()
            if (currentUid == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore: Utente non autenticato"
                    )
                }
                return@launch
            }

            try{

                val user = UsersRepository.currentUserProfile.value

                if(user == null){
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Errore: utente non trovato"
                        )
                    }
                    return@launch
                }else if(!user.stato){
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Errore: Utente disattivato"
                        )
                    }
                    return@launch
                }else{
                    val userReviewsReceivedDef = async{ ReviewRepository.getReviewsForUser(currentUid)}
                    val staysDef = async{ BookingRepository.getBookingByGuest(currentUid)}
                    val adsDef = async{ BookingRepository.getBookingByHost(currentUid)}
                    val statsDef = async{ ReviewRepository.getRatingStats(currentUid, ReviewTarget.UTENTE)}

                    val userReviewsReceived = userReviewsReceivedDef.await().getOrThrow()
                    val stays = staysDef.await().getOrThrow()
                    val ads = adsDef.await().getOrThrow()
                    val (rating, numeroRec) = statsDef.await().getOrThrow()
                    val stats = UserStats(
                        ratingMedia = rating,
                        numeroRecensioni = numeroRec,
                        numeroSoggiorni = stays.size,
                        numeroAnnunci = ads.size
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

                }
            }catch (e: Exception){
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore durante caricamento profilo: ${e.message}"
                    )
                }

            }

        }

    }

    fun errorShown(){
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleReviews() {
        _uiState.update { it.copy(isShowingAllReviews = !it.isShowingAllReviews) }
    }


    fun logout(){
        AuthRepository.signOut()
        UsersRepository.clearCache()

        _uiState.update {
            it.copy(user = null,
            userStats = null,
            isLoggedOut = true) }
    }


}