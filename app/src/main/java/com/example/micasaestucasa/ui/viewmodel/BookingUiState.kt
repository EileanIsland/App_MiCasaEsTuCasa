package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Casa

data class BookingUiState(
    val isLoading : Boolean = false,
    val casa: Casa? = null,
    val startDate: Long? = null,
    val endDate : Long? = null,
    val numGuest: Int = 1,
    val totalPrice: Double = 0.0,
    val numNights: Int = 1,
    val status: String = "in attesa",
    val isSuccess: Boolean = false,
    val isFormValid: Boolean = false,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null
    )
