package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.ChatPreview

data class ChatUiState(
    val chats: List<ChatPreview> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
