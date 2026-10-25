package com.example.micasaestucasa.ui.fragment.booking

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
                val casa = casaDef.await().getOrThrow()

                _uiState.update {
                    state -> state.copy(
                        isLoading = false,
                        casa = casa,
                        startDate = booking?.dataInizio ?: state.startDate,
                        endDate = booking?.dataFine ?: state.endDate,
                        numGuest = booking?.numeroOspiti ?: state.numGuest,
                        status = booking?.getDisplayStatus() ?: state.status,
                    )
                }

                if (booking != null) {
                    updateDates(booking.dataInizio, booking.dataFine)
                } else {
                    validateForm()
                }

            }catch(e: Exception){
                handleError(e, "Errore nel caricamento dei dati")
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
            _uiState.update { it.copy(errorMessage = "Impossibile cancellare la prenotazione") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            BookingRepository.deleteBooking(bookingId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { e -> handleError(e, "Errore durante l'eliminazione") }
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


    fun saveBooking(){
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
                    stato = "In attesa",
                    url = state.casa.immagini.firstOrNull() ?: "",
                    titoloCasa = state.casa.titolo
                )
                BookingRepository.saveBooking(booking).getOrThrow()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }catch(e: Exception){
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Errore durante la prenotazione", isSuccess = false) }
            }
        }
    }


    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi per questa operazione. Verifica di essere il proprietario o l'ospite corretto."
        } else {
            "$context: ${e.localizedMessage ?: "Errore imprevisto"}"
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = message, isSuccess = false) }
    }


    fun clearError(){
        _uiState.update { it.copy(errorMessage = null) }
    }


}


