package com.example.micasaestucasa.ui.fragment.activities

import com.example.micasaestucasa.data.model.BookingUi
import com.example.micasaestucasa.data.model.Casa


data class OwnerActivityUiState(
    val isLoading : Boolean = false,
    val bookingList: List<BookingUi> = emptyList(),
    val housesList: List<Casa> = emptyList(),
    val errorMessage : String? = null,
    val isSuccess: Boolean = false,
    val message: String? = null
)
