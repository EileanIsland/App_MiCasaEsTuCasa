package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.ChatPreview
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.data.model.User

data class DetailedChatUiState(
    val chatInfo: ChatPreview? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val otherUser: User? = null,
    val errorMessage: String? = null
    )