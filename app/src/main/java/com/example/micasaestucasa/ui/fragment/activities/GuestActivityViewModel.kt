package com.example.micasaestucasa.ui.fragment.activities

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
    val uiState: StateFlow<GuestActivityUiState> = _uiState.asStateFlow()

    init {
        loadGuestActivities()
    }

    /**
     * Carica le attività dell'ospite (prenotazioni effettuate).
     * Gestisce il mapping parallelo dei dati della casa e dei profili.
     */
    fun loadGuestActivities() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val currentUid = AuthRepository.getCurrentIUD()
                if (currentUid == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Utente non autenticato")
                    }
                    return@launch
                }

                val rawBookings = BookingRepository.getBookingByGuest(currentUid).getOrThrow()

                val mappedBookings = rawBookings.map { booking ->
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
                handleError(e, "Errore durante il caricamento delle attività")
            }
        }
    }

    /**
     * Gestione centralizzata degli errori per la SnackBar.
     */
    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi per visualizzare queste attività."
        } else {
            "$context: ${e.localizedMessage ?: "Errore imprevisto"}"
        }
        _uiState.update {
            it.copy(isLoading = false, errorMessage = message)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}




