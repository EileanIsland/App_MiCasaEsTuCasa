package com.example.micasaestucasa.ui.fragment.publish

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.data.model.Disponibilita
import com.example.micasaestucasa.data.model.EnumHouseType
import com.example.micasaestucasa.data.repository.StorageRepository
import com.example.micasaestucasa.data.repository.TagsRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
                        availableService = defServices.await().getOrThrow(),
                        availableExperience = defExperiences.await().getOrThrow(),
                        availableRule = defRules.await().getOrThrow(),
                        availableCategory = categories,
                        isLoading = false
                    )
                }

            }catch(e : Exception){
                handleError(e, "Errore nel caricamento dei dati iniziali")
            }
        }
    }

    fun loadCasa(houseId: String) {
        _uiState.update { it.copy(isLoading = true, houseId = houseId) }

        viewModelScope.launch {
            CasaRepository.getCasaById(houseId)
                .onSuccess { casa ->
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Casa non trovata"
                            )
                        }
                    }
                }.onFailure {
                    handleError(it, "Errore nel caricamento della casa")
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

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false, isLoading = false) }
        validateForm()
    }


    private fun validateForm() {
        val s = _uiState.value

        Log.d("VALIDATION_DEBUG", """
        --- VERIFICA CAMPI OBBLIGATORI ---
        Titolo valido: ${s.titolo.isNotBlank()} ("${s.titolo}")
        Descrizione valida: ${s.descrizione.isNotBlank()}
        Prezzo valido (>0): ${s.prezzoNotte > 0} (${s.prezzoNotte})
        Città valida: ${s.citta.isNotBlank()} ("${s.citta}")
        Immagini presenti: ${s.images.isNotEmpty()} (Totale: ${s.images.size})
        Tipologia valida: ${s.tipologia.isNotBlank()} ("${s.tipologia}")
        Latitudine impostata (!= 0.0): ${s.latitudine != 0.0} (${s.latitudine})
        ---------------------------------""".trimIndent())

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
        _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }

        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    val currentUid = UsersRepository.getCurrentUid()
                        ?: throw Exception("Utente non autenticato")

                    val localUris = state.images.filter { it.startsWith("content://") || it.startsWith("file://") }
                    val existingRemoteUrls = state.images.filter { it.startsWith("http") }

                    val newlyUploadedUrls = if (localUris.isNotEmpty()) {
                        StorageRepository.upLoadImages(localUris).getOrThrow()
                    } else emptyList()

                    val finalImages = existingRemoteUrls + newlyUploadedUrls

                    val houseId = state.houseId.ifBlank { "" }

                    val nuovaCasa = Casa(
                        id = houseId,
                        proprietarioId = currentUid,
                        immagini = finalImages,
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
                        esperienza = state.esperienza,
                        servizi = state.servizi,
                        regole = state.regole,
                        disponibilita = state.disponibilita,
                        tipoString = state.tipologia
                    )

                    CasaRepository.saveCasa(nuovaCasa)
                        .onSuccess {savedId ->
                            _uiState.update { it.copy(isLoading = false, isSuccess = true, houseId = savedId) }
                        }
                        .onFailure { e ->
                            handleError(e, "Errore durante il salvataggio su database")
                        }

                } catch (e: Exception) {
                    handleError(e, "Errore durante la pubblicazione")
                }
            }
        }
    }


    private fun handleError(e: Throwable, contextMessage: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi necessari per questa operazione."
        } else {
            "$contextMessage: ${e.localizedMessage}"
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = message, isSuccess = false) }
    }


    fun clearError(){
        _uiState.update { it.copy(errorMessage = null) }
    }

}






