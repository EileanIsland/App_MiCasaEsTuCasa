package com.example.micasaestucasa.ui.fragment.auth

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isAdmin: Boolean = false,
    val errorMessage: String? = null

)
