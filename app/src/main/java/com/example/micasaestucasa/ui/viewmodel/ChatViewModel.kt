package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.ChatRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel(){
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init{
        loadChats()
    }

    private fun loadChats(){
        viewModelScope.launch{
            _uiState.update{ it.copy(isLoading = true) }

            try{
                val uid = UsersRepository.getCurrentUid() ?: throw Exception("Utente non loggato")
                val results = ChatRepository.getChatByUser(uid).collect{
                    chats ->
                    _uiState.update{
                        it.copy(isLoading = false, chats = chats, errorMessage = null)
                    }
                }

            }catch(e: Exception){
                _uiState.update{
                    it.copy(isLoading = false, errorMessage = "Errore durante il caricamento delle chat: ${e.message}")
                }

            }
        }

    }


    fun errorShown(){
        _uiState.update{ it.copy(errorMessage = null) }
    }



}