package com.example.micasaestucasa.ui.fragment.profile

data class ModifyProfileUiState(
    val isLoading:Boolean = false,
    val name : String = "",
    val surname : String = "",
    val bio : String = "",
    val profileImageUrl : String = "",
    val newImageUri : String? = null,
    val phoneNumber: String = "",
    val isSuccess:Boolean = false,
    val errorMessage:String? = null,
)