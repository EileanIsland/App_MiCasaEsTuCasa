package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.ChatRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import com.example.micasaestucasa.data.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailedChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailedChatUiState())
    val uiState: StateFlow<DetailedChatUiState> = _uiState.asStateFlow()

    private var currentChatId: String? = null
    private var currentUserId: String? = null

    fun initChat(chatId: String, otherUserId: String){
        currentChatId = chatId
        currentUserId = UsersRepository.getCurrentUid()

        _uiState.update{it.copy(isLoading = true)}

        viewModelScope.launch {
            UsersRepository.getUserById(otherUserId).onSuccess { user->
                _uiState.update{it.copy(otherUser = user)}
            }
        }

        viewModelScope.launch {
            ChatRepository.getMessages(chatId).collect{ messageList->
                _uiState.update{it.copy(messages = messageList, isLoading = false)}
            }
        }

    }


    fun sendTextMessage(text: String){
        val chatId = currentChatId ?: return
        val senderId = currentUserId ?: return

        if(text.isBlank()) return

        val message = Message(text = text, senderId = senderId, type = "text")

        viewModelScope.launch {
            ChatRepository.sendMessage(chatId, message).onFailure {
                _uiState.update{it.copy(errorMessage = it.errorMessage)}
            }
        }


    }

    fun sendImageMessage(imageUri: String){
        val chatId = currentChatId ?: return
        val senderId = currentUserId ?: return

        viewModelScope.launch {
            ChatRepository.uploadImage(chatId, imageUri).onSuccess { url->
                val message = Message(
                    url = url,
                    senderId = senderId,
                    type = "image"
                   )

                ChatRepository.sendMessage(chatId, message).onFailure {
                    _uiState.update{it.copy(errorMessage = it.errorMessage)}
                }
            }.onFailure {
                _uiState.update { it.copy(errorMessage = it.errorMessage) }
            }
        }
    }


    fun clearError(){
        _uiState.update{it.copy(errorMessage = null)}
    }

}