package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.EnumHouseType
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.TagsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    /**
     * Carica i dati iniziali (Tag e Case) in parallelo.
     * Utilizziamo getOrThrow per i metadati critici e Result per le case.
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Avvio caricamenti in parallelo
                val servicesDef = async { TagsRepository.getTagsByType("servizi") }
                val experiencesDef = async { TagsRepository.getTagsByType("esperienza") }
                val tipologieDef = async { EnumHouseType.listEnumHouse() }
                val housesDef = async { CasaRepository.getAllCase() }

                // Attendiamo i metadati (se uno fallisce, andiamo nel catch)
                val services = servicesDef.await().getOrThrow()
                val experiences = experiencesDef.await().getOrThrow()
                val tipologie = tipologieDef.await()

                // Gestiamo il risultato delle case
                housesDef.await()
                    .onSuccess { housesList ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                availableService = services,
                                availableExperience = experiences,
                                availableCategory = tipologie,
                                houses = housesList
                            )
                        }
                    }
                    .onFailure { e ->
                        handleError(e, "Errore nel recupero delle case")
                    }

            } catch (e: Exception) {
                handleError(e, "Errore durante il caricamento iniziale")
            }
        }
    }

    /**
     * Esegue la ricerca filtrata.
     */
    fun performSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val s = _uiState.value

            CasaRepository.searchHouses(
                query = s.query,
                numPerson = s.numPerson,
                date = s.date,
                services = s.services,
                experiences = s.experiences,
                priceRange = s.priceRange,
                numBathroom = s.numBathroom,
                numBeds = s.numBeds,
                category = s.category
            ).onSuccess { filteredHouses ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        houses = filteredHouses,
                        isFilteredApplied = s.query.isNotEmpty() || s.numPerson > 0 ||
                                s.category.isNotEmpty() || s.services.isNotEmpty()
                    )
                }
            }.onFailure { e ->
                handleError(e, "Errore durante la ricerca")
            }
        }
    }

    /**
     * Gestione centralizzata degli errori (incluso PERMISSION_DENIED).
     */
    private fun handleError(e: Throwable, prefix: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Accesso negato: non hai i permessi per visualizzare questi dati."
        } else {
            "$prefix: ${e.localizedMessage}"
        }
        _uiState.update {
            it.copy(isLoading = false, errorMessage = message)
        }
    }

    fun updateQuery(v: String) { _uiState.update { it.copy(query = v) }; performSearch() }
    fun updateCategory(v: String) { _uiState.update { it.copy(category = v) }; performSearch() }
    fun updateGuests(v: Int) { _uiState.update { it.copy(numPerson = v) }; performSearch() }
    fun updateDates(v: List<Long>) { _uiState.update { it.copy(date = v) }; performSearch() }
    fun updateServices(v: List<String>) { _uiState.update { it.copy(services = v) }; performSearch() }
    fun updateExperiences(v: List<String>) { _uiState.update { it.copy(experiences = v) }; performSearch() }
    fun updatePriceRange(v: List<Int>) { _uiState.update { it.copy(priceRange = v) }; performSearch() }
    fun updateBeds(v: Int) { _uiState.update { it.copy(numBeds = v) }; performSearch() }
    fun updateBathroom(v: Int) { _uiState.update { it.copy(numBathroom = v) }; performSearch() }

    fun nofilter() {
        _uiState.update {
            it.copy(
                query = "", numPerson = 0, date = emptyList(), services = emptyList(),
                experiences = emptyList(), priceRange = listOf(0, 1000000),
                numBathroom = 0, numBeds = 0, category = "", isFilteredApplied = false
            )
        }
        performSearch()
    }

    fun errorShown() { _uiState.update { it.copy(errorMessage = null) } }
}


