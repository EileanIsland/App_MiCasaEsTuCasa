package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivitiesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ActivitiesUiState())
    val uiState : StateFlow<ActivitiesUiState> = _uiState.asStateFlow()


    init{
        loadAllActivities()
    }

    fun loadAllActivities(){
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true) }

            try{
                val currentUid = UsersRepository.getCurrentUid()
                if(currentUid == null){
                    _uiState.update {
                        it.copy(errorMessage = "Utente non loggato") }
                    return@launch
                }

                //TODO: traformare i repository con result<Unit>
                val bookingDef = async{BookingRepository.getBookingByGuest(currentUid)}
                val recBookingDef = async{BookingRepository.getBookingByHost(currentUid)}
                val myHousesDef = async{ CasaRepository.getCaseByProprietario(currentUid)}

                val booking = bookingDef.await()
                val recBooking = recBookingDef.await()
                val myHouses = myHousesDef.await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        myHouses = myHouses,
                        receivedBooking = recBooking,
                        bookingList = booking
                    ) }

            }catch(e : Exception){
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Errore durante il caricamento") }
            }
        }
    }

    fun clearError(){
        _uiState.update {
            it.copy(errorMessage = null) }
    }


}

