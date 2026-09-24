package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.data.model.Disponibilita
import com.example.micasaestucasa.data.model.EnumHouseType
import com.example.micasaestucasa.data.repository.StorageRepository
import com.example.micasaestucasa.data.repository.TagsRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.async
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//TODO:
// 1 - pubblicazione annuncio ora funziona, devo migliorare la navigazione alla pagina di dettaglio
//      ci sono tempi di latenza tra pubblicazione annuncio e risultato che non sono l'idealte
// 2 - Capire come mai non funziona il searchFragment
// 3 - rivedere metodo Save()

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
            try {
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
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Errore durante il caricamento della casa: ${e.message}") }
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

        // Log diagnostico per vedere lo stato dei singoli campi obbligatori
        android.util.Log.d("VALIDATION_DEBUG", """
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
            try {
                // Utilizziamo conContext(NonCancellable) per proteggere l'intero processo di pubblicazione
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    android.util.Log.d("FIRESTORE_TRACE", "1. Inizio salvataggio protetto. Controllo UID...")
                    val currentUid = UsersRepository.getCurrentUid()
                    if (currentUid == null) {
                        _uiState.update { it.copy(errorMessage = "Utente non autenticato", isLoading = false) }
                        return@withContext
                    }

                    android.util.Log.d("FIRESTORE_TRACE", "2. Recupero immagini locali...")
                    val localUris = state.images.filter { it.startsWith("content://") || it.startsWith("file://") }
                    val existingRemoteUrls = state.images.filter { it.startsWith("http") }

                    android.util.Log.d("FIRESTORE_TRACE", "3. Avvio upload su Storage di ${localUris.size} foto...")
                    val uploadResult = StorageRepository.upLoadImages(localUris)
                    val newlyUploadedUrls = uploadResult.getOrNull() ?: emptyList()
                    val finalImages = existingRemoteUrls + newlyUploadedUrls

                    android.util.Log.d("FIRESTORE_TRACE", "4. Foto pronte (Totale: ${finalImages.size}). Creo oggetto Casa...")
                    val houseId = if (state.houseId.isNotBlank()) state.houseId else UUID.randomUUID().toString()

                    val nuovaCasa = Casa(
                        id = houseId,
                        proprietarioId = currentUid,
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
                        immagini = finalImages,
                        tipoString = state.tipologia
                    )

                    android.util.Log.d("FIRESTORE_TRACE", "5. Chiamata a CasaRepository.saveCasa()...")
                    CasaRepository.saveCasa(nuovaCasa)

                    android.util.Log.d("FIRESTORE_TRACE", "6. SALVATAGGIO RIUSCITO CON SUCCESSO!")
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }

            } catch (e: Exception) {
                android.util.Log.e("FIRESTORE_TRACE", "!!! ERRORE DURANTE LA PUBBLICAZIONE !!!", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage, isLoading = false) }
            }
        }
    }



    /*
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
                    _uiState.update { it.copy(errorMessage = "Utente non autenticato") }
                    return@launch
                }

                val localUris = state.images.filter { it.startsWith("content://") || it.startsWith("file://") }
                val existingRemoteUrls = state.images.filter { it.startsWith("http") }
                val newlyUploadedUrls = StorageRepository.upLoadImages(localUris).getOrThrow()

                val remoteImageUrls = existingRemoteUrls + newlyUploadedUrls

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
                        immagini = remoteImageUrls,
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
                        immagini = remoteImageUrls,
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

*/
    fun clearError(){
        _uiState.update { it.copy(errorMessage = null) }
    }

}






