package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.ReviewTarget
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.ReviewRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailedHouseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DetailedHouseUiState())
    val uiState: StateFlow<DetailedHouseUiState> = _uiState.asStateFlow()

    /**
     * Carica i dettagli completi della casa, incluse le recensioni e i dati del proprietario.
     * Utilizza async per massimizzare la velocità di caricamento parallelo.
     */
    fun loadHouseById(id: String) {
        _uiState.update {
            it.copy(isLoading = true, errorMessage = null)
        }

        viewModelScope.launch {
            try {
                val houseDeferred = async { CasaRepository.getCasaById(id) }
                val reviewsDeferred = async { ReviewRepository.getReviewsForHouse(id) }

                val foundHouse = houseDeferred.await().getOrThrow()
                val houseReviews = reviewsDeferred.await().getOrThrow()


                if (foundHouse != null) {
                    val ownerId = foundHouse.proprietarioId
                    val currentUid = AuthRepository.getCurrentIUD()
                    val isOwner = ownerId == currentUid

                    val ownerDeferred = async { UsersRepository.getUserById(ownerId) }
                    val ownerReviewsDeferred = async { ReviewRepository.getReviewsForUser(ownerId) }

                    val owner = ownerDeferred.await().getOrThrow()
                    val ownerReviews = ownerReviewsDeferred.await().getOrThrow()

                    val userStats = UserStats(
                        ratingMedia = owner?.valutazioneMedia,
                        numeroRecensioni = owner?.numeroRecensioni,
                        numeroSoggiorni = owner?.numeroSoggiorni,
                        numeroAnnunci = owner?.numeroAnnunci
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            house = foundHouse,
                            owner = owner,
                            ownerReviews = ownerReviews,
                            houseReviews = houseReviews,
                            ownerStat = userStats,
                            houseRating = foundHouse.valutazioneMedia,
                            numRec = foundHouse.numeroRecensioni,
                            isHouseNotFound = false,
                            isOwner = isOwner
                        )
                    }

                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isHouseNotFound = true,
                            errorMessage = "Questa casa non è più disponibile"
                        )
                    }
                }
            } catch (e: Exception) {
                handleError(e, "Errore durante il caricamento della casa")
            }
        }
    }

    /**
     * Traduce l'eccezione in un messaggio leggibile per la SnackBar.
     */
    private fun handleError(e: Throwable, context: String) {
        android.util.Log.e("DEBUG_DETAILED_HOUSE", "$context: ${e.message}", e)

        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi necessari per visualizzare questi dettagli."
        } else {
            "$context: ${e.localizedMessage ?: "Errore imprevisto"}"
        }

        _uiState.update {
            it.copy(isLoading = false, errorMessage = message)
        }
    }

    fun toggleReviews() {
        _uiState.update { it.copy(isShowingAllReviews = !it.isShowingAllReviews) }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}



