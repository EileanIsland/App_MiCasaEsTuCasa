package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.BookingUi
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OwnerActivityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OwnerActivityUiState())
    val uiState: StateFlow<OwnerActivityUiState> = _uiState.asStateFlow()

    init {
        loadOwnerActivities()
    }

    /**
     * Carica i dati delle attività del proprietario (le sue case e le prenotazioni ricevute).
     */
    fun loadOwnerActivities() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val currentUid = UsersRepository.getCurrentUid()
                if (currentUid == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Utente non autenticato")
                    }
                    return@launch
                }

                val housesDeferred = async { CasaRepository.getCaseByProprietario(currentUid) }
                val bookingsDeferred = async { BookingRepository.getBookingByHost(currentUid) }

                val housesList = housesDeferred.await().getOrThrow()
                val bookings = bookingsDeferred.await().getOrThrow()

                val mappedBookings = bookings.map { booking ->
                    async {
                        try {
                            val casa = CasaRepository.getCasaById(booking.idCasa).getOrThrow()
                            val owner = UsersRepository.getUserById(casa?.proprietarioId ?: "").getOrThrow()
                            val guest = UsersRepository.getUserById(booking.idUtente).getOrThrow()

                            BookingUi(
                                booking = booking,
                                casa = casa,
                                owner = owner,
                                guest = guest
                            )
                        } catch (e: Exception) {
                            // Se fallisce il recupero di un dettaglio di una riga,
                            // restituisce un oggetto parziale per non rompere l'intera lista
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
                handleError(e, "Errore durante il caricamento delle attività")
            }
        }
    }

    /**
     * Disattiva un annuncio (Soft Delete)
     */
    fun deleteHouse(houseId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            CasaRepository.deactivateCasa(houseId)
                .onSuccess {
                    loadOwnerActivities() // Ricarichiamo i dati aggiornati
                }
                .onFailure { e ->
                    handleError(e, "Errore durante l'eliminazione dell'annuncio")
                }
        }
    }

    /**
     * Accetta una prenotazione (Incrementa anche il contatore soggiorni dell'utente)
     */
    fun acceptBooking(bookingId: String) {
        if (bookingId.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            BookingRepository.acceptBooking(bookingId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    loadOwnerActivities()
                }
                .onFailure { e ->
                    handleError(e, "Errore durante la conferma della prenotazione")
                }
        }
    }

    /**
     * Rifiuta una prenotazione
     */
    fun refuseBooking(bookingId: String) {
        if (bookingId.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            BookingRepository.refuseBooking(bookingId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    loadOwnerActivities()
                }
                .onFailure { e ->
                    handleError(e, "Errore durante il rifiuto della prenotazione")
                }
        }
    }

    /**
     * Gestione centralizzata degli errori per intercettare PERMISSION_DENIED
     */
    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi per gestire queste attività. Verifica il login."
        } else {
            "$context: ${e.localizedMessage ?: "Errore imprevisto"}"
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}





