package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Casa

data class SearchUiState(
    val isLoading: Boolean = true,
    val houses: List<Casa> = emptyList(),

    val query: String = "",
    val numPerson: Int = 0,
    val date: List<Long> = emptyList(),

    val availableService : List<String> = emptyList(),
    val availableExperience : List<String> = emptyList(),
    val availableCategory : List<String> = emptyList(),

    val services: List<String> = emptyList(),
    val experiences: List<String> = emptyList(),
    val priceRange: List<Int> = listOf(0, 1000),
    val numBeds: Int = 0,
    val numBathroom: Int = 0,
    val numRange: Int = 0,
    val category: String = "",

    val isFilteredApplied: Boolean = false,
    val errorMessage: String? = null
)
