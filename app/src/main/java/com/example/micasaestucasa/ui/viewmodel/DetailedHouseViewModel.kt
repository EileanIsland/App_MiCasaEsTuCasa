package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.ReviewTarget
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

    fun loadHouseById(id: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            try {

                val houseDeferred = async{ CasaRepository.getCasaById(id) }
                val reviewsDeferred = async { ReviewRepository.getReviewsForHouse(id) }
                val houseStatsDeferred = async { ReviewRepository.getRatingStats(id, ReviewTarget.CASA) }

                val foundHouse = houseDeferred.await()
                val houseReviews = reviewsDeferred.await()
                val houseStats = houseStatsDeferred.await()

                if (foundHouse != null) {
                    val ownerId = foundHouse.proprietarioId

                    val ownerDeferred = async{UsersRepository.getUserById(foundHouse.proprietarioId)}
                    val ownerReviewsDeferred = async { ReviewRepository.getReviewsForUser(ownerId) }
                    val ownerStatsDeferred = async { ReviewRepository.getRatingStats(ownerId, ReviewTarget.UTENTE) }

                    val houseCountDeferred = async { CasaRepository.getCountHousesByOwner(ownerId) }
                    val bookingCountDeferred = async { BookingRepository.getBookingCountByGuest(ownerId) }


                    val owner = ownerDeferred.await().getOrThrow() //TODO VERIFICARE SE IL NUOVO METODO FUNZIONA
                    val ownerReviews = ownerReviewsDeferred.await()
                    val (mediaOwner, totaleOwner) = ownerStatsDeferred.await()
                    val numeroCase = houseCountDeferred.await()
                    val numeroSoggiorni = bookingCountDeferred.await()

                    val userStats = UserStats(
                        ratingMedia = mediaOwner,
                        numeroRecensioni = totaleOwner,
                        numeroSoggiorni = numeroSoggiorni,
                        numeroAnnunci = numeroCase

                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            house = foundHouse,
                            owner = owner,
                            ownerReviews = ownerReviews,
                            houseReviews = houseReviews,
                            ownerStat = userStats,
                            houseRating = houseStats.first,
                            numRec = houseStats.second,
                            isHouseNotFound = false,
                        )
                    }

                } else {
                    //CaSA non trovata
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isHouseNotFound = true,
                            errorMessage = "Casa non trovata"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore durante il caricamento della casa"
                    )
                }
            }

        }
    }



    fun toggleReviews() {
        _uiState.update { it.copy(isShowingAllReviews = !it.isShowingAllReviews) }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }


}
