package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Booking
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.data.model.ChatPreview
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.data.model.User

data class DetailedChatUiState(
    val isLoading: Boolean = false,
    val chatInfo: ChatPreview? = null,
    val messages: List<Message> = emptyList(),
    val otherUser: User? = null,
    val errorMessage: String? = null,
    val userFeedback: String? = null,

    //QUESTE MI SERVONO PER IL CONTESTO - ES CHIEDO INFO SU UNA CASA, contesto -> house
    val house: Casa? = null,
    val booking: Booking? = null,

    //NUOVE PER GESTIRE STORICO PRENOTAZIONI
    val bookingsMap: Map<String, Booking> = emptyMap(),
    val isHost: Boolean = false,

    val actionSuccess: Boolean = false,    // Utile per chiudere la chat dopo l'eliminazione
    val canReviewHouse: Boolean = false,       // Determina se mostrare l'icona/pulsante recensione
)
