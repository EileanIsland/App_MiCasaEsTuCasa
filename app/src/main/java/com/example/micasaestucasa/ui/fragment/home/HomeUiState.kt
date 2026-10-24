package com.example.micasaestucasa.ui.fragment.home

import com.example.micasaestucasa.data.model.Casa

data class HomeUiState(
    val isLoading: Boolean = true,
    val houses: List<Casa> = emptyList(),
    val errorMessage: String? = null,
)
