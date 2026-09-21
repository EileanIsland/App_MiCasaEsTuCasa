package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Booking
import com.example.micasaestucasa.data.model.Casa

data class ActivitiesUiState(
    val isLoading : Boolean = false,
    val myHouses : List<Casa> = emptyList(),
    val receivedBooking : List<Booking> = emptyList(),
    val bookingList : List<Booking> = emptyList(),
    val errorMessage : String? = null,
    )
