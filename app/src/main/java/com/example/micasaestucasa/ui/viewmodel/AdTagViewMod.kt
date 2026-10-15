package com.example.micasaestucasa.ui.viewmodel

import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.TagsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdTagUiState(
    val isLoading: Boolean = false,
    val services: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val rules: List<String> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedTags: Set<Pair<String, String>> = emptySet(),
    val userFeedback: String = "",
    val errorMessage: String = ""

)


class AdTagViewMod : ViewModel() {
    private val _uiState = MutableStateFlow(AdTagUiState())
    val uiState: StateFlow<AdTagUiState> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    fun loadTags(){
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val servicesDef = async{TagsRepository.getTagsByType("servizi")}
                val featuresDef = async{TagsRepository.getTagsByType("esperienza")}
                val rulesDef = async{TagsRepository.getTagsByType("regole")}

                val serv = servicesDef.await().getOrThrow()
                val feat = featuresDef.await().getOrThrow()
                val rul = rulesDef.await().getOrThrow()

                _uiState.value = _uiState.value.copy(
                    services = serv,
                    features = feat,
                    rules = rul,
                    isLoading = false
                )
            }catch (e: Exception){
                handleError(e, "Errore nel caricamento dei tags")
            }

        }
    }

    private fun handleError(e: Throwable, context: String){
        if(e.message?.contains("PERMISSION_DENIED") == true){
            _uiState.value = _uiState.value.copy(errorMessage = "Accesso negato: non hai i permessi per questa operazione.")
            return
        }else{
            val message = "$context: ${e.localizedMessage ?: "Errore imprevisto"}"
            _uiState.value = _uiState.value.copy(errorMessage = message)

        }
    }

    fun saveTag(type: String, tag: String){
        viewModelScope.launch {
            TagsRepository.saveTag(tag, type).onSuccess {
                loadTags()
                _uiState.value = _uiState.value.copy(userFeedback = "Tag inserito correttamente")
            }.onFailure { e ->
                handleError(e, "Errore durante l'inserimento del tag")
            }
        }
    }


    fun toggleSelectionMode() {
        _uiState.update { it.copy(isSelectionMode =  !(it.isSelectionMode), selectedTags = emptySet()) }
    }

    fun toggleSelection(item: Pair<String, String>) {
        _uiState.update { state ->
            val currentSelection = state.selectedTags.toMutableSet()

            if (currentSelection.contains(item)) {
                currentSelection.remove(item)
            } else {
                currentSelection.add(item)
            }

            state.copy(selectedTags = currentSelection)
        }
    }

    fun deleteSelectedTags() {
        val tagsToDelete = _uiState.value.selectedTags
        if (tagsToDelete.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            var hasError = false

            tagsToDelete.forEach { (category, tagName) ->
                android.util.Log.d("TAG_DEBUG - view", "Eliminazione tag: $tagName - $category")
                TagsRepository.deleteTag(tagName, category).onFailure {
                    hasError = true
                }
            }

            if (hasError) {
                handleError(Exception("Alcuni tag non sono stati eliminati"), "Errore eliminazione multipla")
            } else {
                _uiState.update { it.copy(userFeedback = "Tag eliminati con successo") }
            }

            loadTags()
            toggleSelectionMode()
        }
    }


    fun clearError(){
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }

    fun resetFeedback(){
        _uiState.value = _uiState.value.copy(userFeedback = "")
    }

}



