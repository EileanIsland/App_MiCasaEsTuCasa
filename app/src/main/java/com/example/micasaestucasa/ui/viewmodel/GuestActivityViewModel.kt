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

class GuestActivityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GuestActivityUiState())
    val uiState : StateFlow<GuestActivityUiState> = _uiState.asStateFlow()

    init {
        loadGuestActivities()
    }

    fun loadGuestActivities() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1. Recupero l'ID dell'utente attualmente loggato
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

                val rawBookings = BookingRepository.getBookingByGuest(currentUid)//.getOrThrow()

                // 3. Mappatura in PARALLELO di ogni singola prenotazione
                // .map restituisce una lista di Deferred<BookingUi> grazie al blocco async
                val mappedBookings = rawBookings.map { booking ->
                    async {
                        try {
                            // Scarico la casa associata alla prenotazione
                            val casa = CasaRepository.getCasaById(booking.idCasa)//.getOrThrow()

                            // Scarico il proprietario della casa (Owner)
                            val owner = UsersRepository.getUserById(casa?.proprietarioId ?: "").getOrThrow()

                            // Scarico il profilo di chi ha prenotato (Guest)
                            val guest = UsersRepository.getUserById(booking.idUtente).getOrThrow()

                            BookingUi(
                                booking = booking,
                                casa = casa,
                                owner = owner,
                                guest = guest
                            )
                        } catch (e: Exception) {
                            BookingUi(
                                booking = booking,
                                casa = null,
                                owner = null,
                                guest = null
                            )
                        }
                    }
                }.awaitAll()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookingList = mappedBookings
                    )
                }

            } catch (e: Exception) {
                // Cattura eventuali errori generali (es. fallimento del BookingRepository iniziale)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Errore durante il caricamento delle attività"
                    )
                }
            }
        }
    }

    fun clearError(){
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}



