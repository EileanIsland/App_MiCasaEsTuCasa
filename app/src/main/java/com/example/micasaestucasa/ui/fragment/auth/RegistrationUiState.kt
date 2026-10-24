package com.example.micasaestucasa.ui.fragment.auth


data class RegistrationUiState(
    val isLoading: Boolean = false,
    val isSucess: Boolean = false,
    val errorMessage: String? = null
)
