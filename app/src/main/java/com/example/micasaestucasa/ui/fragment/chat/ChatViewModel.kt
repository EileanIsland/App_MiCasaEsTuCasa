package com.example.micasaestucasa.ui.fragment.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.ChatRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.flow.*


class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        val uid = UsersRepository.getCurrentUid()

        if (uid == null) {
            _uiState.update { it.copy(errorMessage = "Utente non loggato", isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        ChatRepository.getChatByUser(uid)
            .onEach { chats ->
                _uiState.update {
                    it.copy(isLoading = false, chats = chats, errorMessage = null)
                }
            }
            .catch { e ->
                handleError(e, "Errore nel caricamento delle chat")
            }
            .launchIn(viewModelScope)
    }

    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi per visualizzare queste conversazioni."
        } else {
            "$context: ${e.localizedMessage ?: "Errore imprevisto"}"
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}





