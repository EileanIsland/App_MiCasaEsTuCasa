package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.Booking
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

class BookingViewModel : ViewModel(){
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private var currentBookingId : String? = null //id prenotazione per modalità modifica

    fun loadBookingData(houseId: String, bookingId: String? = null) {
        currentBookingId = bookingId
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch{
            try{
                val casaDef = async {CasaRepository.getCasaById(houseId)}
                val bookingDeferred = if (bookingId != null) {
                    async { BookingRepository.getBookingById(bookingId).getOrNull() }
                } else null

                val booking = bookingDeferred?.await()
                val casa = casaDef.await()

                _uiState.update {
                    state -> state.copy(
                        isLoading = false,
                        casa = casa,
                        startDate = booking?.dataInizio ?: state.startDate,
                        endDate = booking?.dataFine ?: state.endDate,
                        numGuest = booking?.numeroOspiti ?: state.numGuest,
                        status = booking?.stato ?: state.status,
                    )
                }

                if (booking != null) {
                    updateDates(booking.dataInizio, booking.dataFine)
                } else {
                    validateForm()
                }

            }catch(e: Exception){
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }

        }
    }

    fun updateDates(startDate: Long, endDate: Long){
        val diff = endDate - startDate
        val nights = (diff / (1000 * 60 * 60 * 24)).toInt()
        val prNight = uiState.value.casa?.prezzoNotte ?: 0.0
        _uiState.update { it.copy(startDate = startDate, endDate = endDate, numNights = nights, totalPrice = nights * prNight) }
        validateForm()

    }

    fun updateGuest(numGuest: Int){
        val max = _uiState.value.casa?.ospitiMassimi ?: 1
        val validatedCount = if (numGuest > max) max else numGuest

        _uiState.update { it.copy(numGuest = validatedCount) }

        validateForm()

    }

    fun deleteBooking(bookingId: String){
        if(bookingId.isEmpty()) return

        if(_uiState.value.status != "In attesa"){
            _uiState.update { it.copy(errorMessage = "Impossibile eliminare una prenotazione") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                BookingRepository.deleteBooking(bookingId).getOrThrow()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                            ?: "Errore durante la cancellazione della prenotazione"
                    )
                }
            }
        }


    }

    private fun validateForm(){
        val s = _uiState.value
        val isValid =
            s.casa != null &&
                    s.startDate != null
                    && s.endDate != null
                    && s.startDate < s.endDate
                    && s.numGuest > 0
                    && s.numGuest <= s.casa.ospitiMassimi
                    && s.numNights> 0

        _uiState.update { it.copy(isFormValid = isValid) }
    }


    fun confirmBooking(){
        val state = _uiState.value
        if(!state.isFormValid) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch{
            try{
                val currentUid = UsersRepository.getCurrentUid() ?: throw Exception("Effettuare il login per prenotare")
                val booking = Booking(
                    idBooking = currentBookingId ?: "",
                    idCasa = state.casa!!.id,
                    idUtente = currentUid,
                    idHost = state.casa.proprietarioId,
                    dataInizio = state.startDate!!,
                    dataFine = state.endDate!!,
                    prezzoTotale = state.totalPrice,
                    numeroOspiti = state.numGuest,
                    stato = "In attesa"
                )
                BookingRepository.saveBooking(booking).getOrThrow()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }catch(e: Exception){
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Errore durante la prenotazione", isSuccess = false) }
            }
        }
    }


    fun clearError(){
        _uiState.update { it.copy(errorMessage = null) }
    }


}
