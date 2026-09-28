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

class BookingViewModel : ViewModel(){
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun load(houseId: String){
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch{
            try{
                val casa = CasaRepository.getCasaById(houseId)
                _uiState.update { it.copy(isLoading = false, casa = casa) }


            }catch(e: Exception){
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }

        }
    }

    fun updateDates(startDate: Long, endDate: Long){
        val diff = endDate -startDate
        val nights = (diff / (1000 * 60 * 60 * 24)).toInt()
        val prNight = uiState.value.casa?.prezzoNotte ?: 0.0
        validateForm()

        _uiState.update { it.copy(startDate = startDate, endDate = endDate, numNights = nights, totalPrice = nights * prNight) }
    }

    fun updateGuest(numGuest: Int){
        validateForm()
        _uiState.update { it.copy(numGuest = numGuest) }
    }

    private fun validateForm(){
        val s = _uiState.value
        val isValid = s.casa != null && s.startDate != null && s.endDate != null && s.numGuest > 0 && s.numNights> 0

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
                    idCasa = state.casa!!.id,
                    idUtente = currentUid,
                    idHost = state.casa.proprietarioId,
                    dataInizio = state.startDate.toString(),
                    dataFine = state.endDate.toString(),
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
