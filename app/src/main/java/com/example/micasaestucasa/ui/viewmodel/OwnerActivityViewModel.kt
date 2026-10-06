package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.BookingUi
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class OwnerActivityViewModel : ViewModel(){
    private val _uiState = MutableStateFlow(OwnerActivityUiState())
    val uiState : StateFlow<OwnerActivityUiState> = _uiState.asStateFlow()

    init{
        loadOwnerActivities()
    }

    /**
     * Carica i dati nel Uistate per OwnerFragment
     */
    fun loadOwnerActivities() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val currentUid = AuthRepository.getCurrentIUD()
                if (currentUid == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Utente non autenticato"
                        )
                    }
                    return@launch
                }

                val housesDeferred = async { CasaRepository.getCaseByProprietario(currentUid) }
                val bookingsDeferred = async { BookingRepository.getBookingByHost(currentUid) }

                val housesList = housesDeferred.await()
                val bookings = bookingsDeferred.await().getOrThrow()

                val mappedBookings = bookings.map { booking ->
                    async {
                        try {
                            val casa = CasaRepository.getCasaById(booking.idCasa)

                            val owner = UsersRepository.getUserById(casa?.proprietarioId ?: "").getOrThrow()
                            val guest = UsersRepository.getUserById(booking.idUtente).getOrThrow()

                            BookingUi(
                                booking = booking,
                                casa = casa,
                                owner = owner,
                                guest = guest
                            )
                        } catch (e: Exception) {
                            // Se fallisce il recupero di un dettaglio, creiamo comunque l'oggetto
                            // per non far sparire l'intera prenotazione dalla lista
                            BookingUi(booking = booking, casa = null, owner = null, guest = null)
                        }
                    }
                }.awaitAll()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        housesList = housesList,
                        bookingList = mappedBookings
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Errore durante il caricamento delle attività"
                    )
                }
            }
        }
    }

    fun deleteHouse(houseId: String) {
        viewModelScope.launch {_uiState.update { it.copy(isLoading = true) }
            try {
                CasaRepository.deactivateCasa(houseId).getOrThrow()

                loadOwnerActivities()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }


    /**
     * Accetta una prenotazione impostando lo stato su "Confermata"
     */
    fun acceptBooking(bookingId: String){
        if(bookingId.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                BookingRepository.acceptBooking(bookingId).getOrThrow()
                loadOwnerActivities()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            }catch(e: Exception){
                _uiState.update{it.copy(errorMessage=e.message ?: "Errore durante la conferma della prenotazione", isLoading = false, isSuccess = false)}
            }
        }
    }


    fun refuseBooking(bookingId: String){
        if(bookingId.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                BookingRepository.refuseBooking(bookingId).getOrThrow()
                loadOwnerActivities()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            }catch(e: Exception){
                _uiState.update{it.copy(errorMessage=e.message ?: "Errore durante la gestione della prenotazione", isLoading = false, isSuccess = false)}
            }
        }

    }

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun clearMessage(){
        _uiState.update{
            it.copy(message = null)
        }
    }

    fun clearError(){
        _uiState.update{
            it.copy(errorMessage = null)
        }
    }

}



