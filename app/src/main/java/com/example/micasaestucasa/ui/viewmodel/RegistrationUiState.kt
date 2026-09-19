package com.example.micasaestucasa.ui.viewmodel


data class RegistrationUiState(
    val isLoading: Boolean = false,
    val isSucess: Boolean = false,
    val errorMessage: String? = null
)
