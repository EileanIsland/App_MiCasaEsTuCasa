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

//TODO: mappa non funziona

class SearchViewModel : ViewModel(){

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init{
        loadInitialData()
    }

    private fun loadInitialData(){
        viewModelScope.launch{
            _uiState.update{
                it.copy(isLoading = true)
            }

            try{
                val servicesDeferred = async { TagsRepository.getTagsByType("servizi") }
                val experiencesDeferred = async { TagsRepository.getTagsByType("esperienza")  }
                val tipologie = async { EnumHouseType.listEnumHouse() }
                val houseDeferred = async{ CasaRepository.getAllCase() }

                val services = servicesDeferred.await()
                val experiences = experiencesDeferred.await()
                val houses = houseDeferred.await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        availableService = services,
                        availableExperience = experiences,
                        availableCategory = tipologie.await(),
                        houses = houses
                    )
                }
            }catch(e: Exception){
                _uiState.update{
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore durante il caricamento iniziale: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateQuery(query: String){
        _uiState.update{ it.copy(query = query) }
        performSearch()
    }

    fun updateGuests(guests: Int){
        _uiState.update{ it.copy(numPerson = guests) }
        performSearch()
    }

    fun updateDates(dates: List<Long>){
        _uiState.update{ it.copy(date = dates) }
        performSearch()
    }

    fun updateServices(services: List<String>){
        _uiState.update{ it.copy(services = services) }
        performSearch()
    }

    fun updateExperiences(experiences: List<String>){
        _uiState.update{ it.copy(experiences = experiences) }
        performSearch()
    }

    fun updatePriceRange(priceRange: List<Int>){
        _uiState.update{ it.copy(priceRange = priceRange) }
        performSearch()
    }

    fun updateBeds(beds: Int){
        _uiState.update{ it.copy(numBeds = beds) }
        performSearch()
    }

    fun updateBathroom(bathroom: Int){
        _uiState.update{ it.copy(numBathroom = bathroom) }
        performSearch()
    }

    fun updateCategory(category: String){
        _uiState.update{ it.copy(category = category) }
        performSearch()
    }

    fun nofilter(){
        _uiState.update{
            it.copy(
                query = "",
                numPerson = 0,
                date = emptyList(),
                services = emptyList(),
                experiences = emptyList(),
                priceRange = listOf(0, 1000),
                numBathroom = 0,
                numBeds = 0,
                category = "",
                isFilteredApplied = false
            )
        }
        performSearch()
    }

    fun performSearch(){
        viewModelScope.launch{
            _uiState.update{
                it.copy(isLoading = true)
            }

            try{
                val currentState = _uiState.value

                val houses = CasaRepository.searchHouses(
                    query = currentState.query,
                    numPerson = currentState.numPerson,
                    date = currentState.date,
                    services = currentState.services,
                    experiences = currentState.experiences,
                    priceRange = currentState.priceRange,
                    numBathroom = currentState.numBathroom,
                    numBeds = currentState.numBeds,
                    category = currentState.category
                )

                _uiState.update{
                    it.copy(
                        isLoading = false,
                        houses = houses,
                        isFilteredApplied = currentState.query.isNotEmpty() ||
                                currentState.numPerson > 0 ||
                                currentState.category.isNotEmpty() ||
                                currentState.services.isNotEmpty()

                    )
                }

            }catch(e: Exception){
                _uiState.update{
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore durante la ricerca: ${e.message}"
                    )
                }

            }
        }

    }

    fun errorShown(){
        _uiState.update{ it.copy(errorMessage = null) }
    }
}