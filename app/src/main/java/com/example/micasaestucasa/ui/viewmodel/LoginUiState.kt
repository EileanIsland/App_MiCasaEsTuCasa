package com.example.micasaestucasa.ui.viewmodel

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isAdmin: Boolean = false,
    val errorMessage: String? = null

)
