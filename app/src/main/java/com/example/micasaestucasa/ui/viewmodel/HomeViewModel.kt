package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.CasaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHouses()
    }

    /**
     * Carica le case con la valutazione più alta.
     * Utilizza il pattern Result per gestire in modo sicuro errori di rete o permessi.
     */
    fun loadHouses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // CasaRepository.getTopRatedHouses() ora restituisce Result<List<Casa>>
            CasaRepository.getTopRatedHouses()
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            houses = list
                        )
                    }
                }
                .onFailure { error ->
                    handleError(error)
                }
        }
    }

    /**
     * Gestione centralizzata dell'errore, allineata alle regole Firestore.
     */
    private fun handleError(e: Throwable) {
        val msg = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi necessari per visualizzare le case in evidenza."
        } else {
            e.localizedMessage ?: "Si è verificato un errore nel caricamento delle case."
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = msg
            )
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}





