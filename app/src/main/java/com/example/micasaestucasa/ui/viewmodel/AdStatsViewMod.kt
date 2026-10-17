package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdStatsUiState(
    val isLoading: Boolean = false,
    val total: Int = 0,
    val concluded: Int = 0,
    val ongoing: Int = 0,
    val upcoming: Int = 0,
    val pending: Int = 0,
    val errorMessage: String? = null


)

class AdStatsViewMod : ViewModel(){
    private var _uiState = MutableStateFlow(AdStatsUiState())
    val uiState: StateFlow<AdStatsUiState> = _uiState.asStateFlow()

    init{
        loadStats()
    }

    private fun loadStats(){
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            AdminRepository.getBookingStats()
                .onSuccess { stats->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        total = stats["total"] ?: 0,
                        concluded = stats["concluded"] ?: 0,
                        ongoing = stats["ongoing"] ?: 0,
                        upcoming = stats["upcoming"] ?: 0,
                        pending = stats["pending"] ?: 0
                    )

                }.onFailure {
                    _uiState.value = _uiState.value.copy(errorMessage = it.message)
                }
        }
    }
}