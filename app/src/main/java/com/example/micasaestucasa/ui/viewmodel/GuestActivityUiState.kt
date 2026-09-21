package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.BookingUi

data class GuestActivityUiState(
    val isLoading : Boolean = false,
    val bookingList: List<BookingUi> = emptyList(),
    val errorMessage : String? = null,
)
