package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.EnumHouseType
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.TagsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel(){

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init{
        loadInitialData()
    }

    private fun loadInitialData(){
        viewModelScope.launch{
            _uiState.update{ it.copy(isLoading = true) }

            try {
                val servicesDeferred = async { TagsRepository.getTagsByType("servizi") }
                val experiencesDeferred = async { TagsRepository.getTagsByType("esperienza") }
                val tipologieDef = async { EnumHouseType.listEnumHouse() }
                val houseDeferred = async { CasaRepository.getAllCase() }

                val services = servicesDeferred.await()
                val experiences = experiencesDeferred.await()
                val tipologie = tipologieDef.await()

                val housesResult = houseDeferred.await()

                housesResult.onSuccess { housesList ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            availableService = services,
                            availableExperience = experiences,
                            availableCategory = tipologie,
                            houses = housesList
                        )
                    }
                }.onFailure { e ->
                    handleError("Errore caricamento case: ${e.message}")
                }

            } catch(e: Exception) {
                handleError("Errore durante il caricamento iniziale: ${e.message}")
            }
        }
    }

    private fun handleError(message: String) {
        val finalMessage = if (message.contains("PERMISSION_DENIED")) {
            "Accesso negato: controlla le regole di sicurezza o il login."
        } else {
            message
        }
        _uiState.update {
            it.copy(isLoading = false, errorMessage = finalMessage)
        }
    }

    fun performSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentState = _uiState.value

            CasaRepository.searchHouses(
                query = currentState.query,
                numPerson = currentState.numPerson,
                date = currentState.date,
                services = currentState.services,
                experiences = currentState.experiences,
                priceRange = currentState.priceRange,
                numBathroom = currentState.numBathroom,
                numBeds = currentState.numBeds,
                category = currentState.category
            ).onSuccess { filteredHouses ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        houses = filteredHouses,
                        isFilteredApplied = currentState.query.isNotEmpty() ||
                                currentState.numPerson > 0 ||
                                currentState.category.isNotEmpty() ||
                                currentState.services.isNotEmpty()
                    )
                }
            }.onFailure { e ->
                handleError("Errore durante la ricerca: ${e.message}")
            }
        }
    }

    // --- Metodi di aggiornamento invariati ---
    fun updateQuery(query: String) { _uiState.update { it.copy(query = query) }; performSearch() }
    fun updateCategory(category: String) { _uiState.update { it.copy(category = category) }; performSearch() }
    fun updateGuests(guests: Int) { _uiState.update { it.copy(numPerson = guests) }; performSearch() }
    fun updateDates(dates: List<Long>) { _uiState.update { it.copy(date = dates) }; performSearch() }
    fun updateServices(services: List<String>) { _uiState.update { it.copy(services = services) }; performSearch() }
    fun updateExperiences(experiences: List<String>) { _uiState.update { it.copy(experiences = experiences) }; performSearch() }
    fun updatePriceRange(priceRange: List<Int>) { _uiState.update { it.copy(priceRange = priceRange) }; performSearch() }
    fun updateBeds(beds: Int) { _uiState.update { it.copy(numBeds = beds) }; performSearch() }
    fun updateBathroom(bathroom: Int) { _uiState.update { it.copy(numBathroom = bathroom) }; performSearch() }

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



