package com.example.micasaestucasa.ui.viewmodel

data class ModifyProfileUiState(
    val isloading:Boolean = false,
    val isSuccess:Boolean = false,
    val errorMessage:String? = null,
)