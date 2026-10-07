package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Booking
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.data.model.ChatPreview
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.data.model.User

data class DetailedChatUiState(
    val chatInfo: ChatPreview? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val otherUser: User? = null,
    val errorMessage: String? = null,

    val house: Casa? = null,
    val booking: Booking? = null,

    val isDeleting: Boolean = false,       // Indica se l'eliminazione è in corso
    val isReporting: Boolean = false,      // Indica se la segnalazione è in corso
    val actionSuccess: Boolean = false,    // Utile per chiudere la chat dopo l'eliminazione
    val userFeedback: String? = null,      // Per mostrare un messaggio (es. "Chat segnalata")

    val canReview: Boolean = false,       // Determina se mostrare l'icona/pulsante recensione
    val hasAlreadyReviewed: Boolean = false, // Evita doppie recensioni
    val isSubmittingReview: Boolean = false, // Loading durante il salvataggio del voto
    val showReviewDialog: Boolean = false    // Controlla la visibilità del popup recensione
)