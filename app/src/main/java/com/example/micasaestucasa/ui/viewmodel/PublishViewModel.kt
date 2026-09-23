package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.data.model.Disponibilita
import com.example.micasaestucasa.data.model.EnumHouseType
import com.example.micasaestucasa.data.repository.TagsRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.async
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class PublishViewModel: ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    private var originalCasa: Casa? = null

    init{
        loadAvailableData()
    }

    private fun loadAvailableData(){
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch{
            try{
                val defServices = async{ TagsRepository.getTagsByType("servizio")}
                val defExperiences =  async{ TagsRepository.getTagsByType("esperienza")}
                val defRules = async{ TagsRepository.getTagsByType("regole")}
                val categories = EnumHouseType.listEnumHouse()

                _uiState.update {
                    it.copy(
                        availableService = defServices.await(),
                        availableExperience = defExperiences.await(),
                        availableRule = defRules.await(),
                        availableCategory = categories,
                        isLoading = it.houseId.isNotBlank()
                    )
                }

            }catch(e : Exception){
                _uiState.update { it.copy(errorMessage = "errore nel caricamento dei dati") }
            }

        }
    }

    fun loadCasa(houseId: String) {
        _uiState.update { it.copy(isLoading = true, houseId = houseId) }

        viewModelScope.launch {
            val casa = CasaRepository.getCasaById(houseId)

            if (casa != null) {
                originalCasa = casa

                _uiState.update {
                    it.copy(
                        houseId = casa.id,
                        titolo = casa.titolo,
                        tipologia = casa.tipo.label,
                        descrizione = casa.descrizione,
                        prezzoNotte = casa.prezzoNotte,
                        citta = casa.citta,
                        indirizzo = casa.indirizzo,
                        latitudine = casa.latitudine,
                        longitudine = casa.longitudine,
                        ospitiMassimi = casa.ospitiMassimi,
                        numeroStanze = casa.numeroCamere,
                        numeroLetti = casa.numeroLetti,
                        numeroBagni = casa.numeroBagni,
                        esperienza = casa.esperienza,
                        servizi = casa.servizi,
                        regole = casa.regole,
                        disponibilita = casa.disponibilita,
                        images = casa.immagini,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Casa non trovata") }
            }
        }
    }

    fun updateTitolo(value: String){
        _uiState.update { it.copy(titolo = value) }
        validateForm()
    }

    fun updateTipologia(value: String){
        _uiState.update { it.copy(tipologia = value) }
        validateForm()
    }

    fun updateDescrizione(value: String){
        _uiState.update { it.copy(descrizione = value) }
        validateForm()
    }

    fun updatePrezzo(value: Double){
        _uiState.update { it.copy(prezzoNotte = value)  }
        validateForm()
    }

    fun updateCitta(value: String){
        _uiState.update { it.copy(citta = value) }
        validateForm()
    }

    fun updateIndirizzo(value: String){
        _uiState.update { it.copy(indirizzo = value) }
        validateForm()
    }

    fun updateLatitudine(value: Double){
        _uiState.update { it.copy(latitudine = value) }
        validateForm()
    }

    fun updateLongitudine(value: Double){
        _uiState.update { it.copy(longitudine = value) }
        validateForm()
    }

    fun updateOspitiMassimi(value: Int){
        _uiState.update { it.copy(ospitiMassimi = value) }
        validateForm()
    }

    fun updateNumeroStanze(value: Int){
        _uiState.update { it.copy(numeroStanze = value) }
        validateForm()
    }

    fun updateNumeroLetti(value: Int){
        _uiState.update { it.copy(numeroLetti = value) }
        validateForm()
    }

    fun updateNumeroBagni(value: Int){
        _uiState.update { it.copy(numeroBagni = value) }
        validateForm()
    }

    fun addImage(uri: String) {
        _uiState.update { it.copy(images = it.images + uri) }
        validateForm()
    }

    fun removeImage(uri: String) {
        _uiState.update { it.copy(images = it.images.filter { it != uri }) }
        validateForm()

    }

    fun addEsperienza(value: String){
        _uiState.update { it.copy(esperienza = it.esperienza + value) }
        validateForm()

    }

    fun removeEsperienza(value: String) {
        _uiState.update { it.copy(esperienza = it.esperienza.filter { it != value }) }
        validateForm()

    }

    fun addServizio(value: String){
        _uiState.update { it.copy(servizi = it.servizi + value) }
        validateForm()

    }

    fun removeServizio(value: String) {
        _uiState.update {
            it.copy(servizi = it.servizi.filter { it != value })
        }
        validateForm()

    }

    fun addRegola(value: String){
        _uiState.update { it.copy(regole = it.regole + value) }
        validateForm()

    }

    fun removeRegola(value: String) {
        _uiState.update { it.copy(regole = it.regole.filter { it != value }) }
        validateForm()

    }

    fun addDisponibilita(value: Disponibilita){
        _uiState.update { it.copy(disponibilita = it.disponibilita + value) }
        validateForm()

    }

    fun removeDisponibilita(value: Disponibilita) {
        _uiState.update { it.copy(disponibilita = it.disponibilita.filter { it != value }) }
        validateForm()
    }


    private fun validateForm() {
        val s = _uiState.value
        val isValid = s.titolo.isNotBlank() &&
                s.descrizione.isNotBlank() &&
                s.prezzoNotte > 0 &&
                s.citta.isNotBlank() &&
                s.images.isNotEmpty() &&
                s.tipologia.isNotBlank() &&
                s.latitudine != 0.0
        _uiState.update { it.copy(isFormValid = isValid) }
    }


    fun save() {
        val state = _uiState.value

        if (!state.isFormValid) {
            _uiState.update { it.copy(errorMessage = "Compilare tutti i campi obbligatori") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {

            try {
                val currentUid = UsersRepository.getCurrentUid()
                if (currentUid == null) {
                    _uiState.update { it.copy(errorMessage = "Utente non loggato") }
                    return@launch
                }

                val casaDaSalvare = if (originalCasa == null) {
                    Casa(
                        id = UUID.randomUUID().toString(),
                        titolo = state.titolo,
                        descrizione = state.descrizione,
                        prezzoNotte = state.prezzoNotte,
                        citta = state.citta,
                        indirizzo = state.indirizzo,
                        latitudine = state.latitudine,
                        longitudine = state.longitudine,
                        ospitiMassimi = state.ospitiMassimi,
                        numeroCamere = state.numeroStanze,
                        numeroLetti = state.numeroLetti,
                        numeroBagni = state.numeroBagni,
                        tipo = EnumHouseType.entries.find { it.label == state.tipologia }
                            ?: EnumHouseType.ALTRO,
                        immagini = state.images,
                        servizi = state.servizi,
                        esperienza = state.esperienza,
                        regole = state.regole,
                        disponibilita = state.disponibilita,
                        proprietarioId = currentUid
                    )
                } else {
                    originalCasa!!.copy(
                        titolo = state.titolo,
                        descrizione = state.descrizione,
                        prezzoNotte = state.prezzoNotte,
                        citta = state.citta,
                        indirizzo = state.indirizzo,
                        latitudine = state.latitudine,
                        longitudine = state.longitudine,
                        ospitiMassimi = state.ospitiMassimi,
                        numeroCamere = state.numeroStanze,
                        numeroLetti = state.numeroLetti,
                        numeroBagni = state.numeroBagni,
                        tipo = EnumHouseType.entries.find { it.label == state.tipologia }
                            ?: EnumHouseType.ALTRO,
                        immagini = state.images,
                        servizi = state.servizi,
                        esperienza = state.esperienza,
                        regole = state.regole,
                        disponibilita = state.disponibilita
                    )
                }

                CasaRepository.saveCasa(casaDaSalvare)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore durante il salvataggio: ${e.message}"
                    )
                }
            }

        }
    }


    fun clearError(){
        _uiState.update { it.copy(errorMessage = null) }
    }

}






