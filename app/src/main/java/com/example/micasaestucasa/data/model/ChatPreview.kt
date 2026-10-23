package com.example.micasaestucasa.data.model

/**
 * Rappresenta un'anteprima di una conversazione nella lista delle chat.
 *
 * @property id Identificativo della chat.
 * @property lastMessage Testo dell'ultimo messaggio inviato o ricevuto.
 * @property lastTimestamp Timestamp dell'ultimo messaggio.
 * @property otherUserPhoto URL della foto profilo dell'altro partecipante.
 * @property otherUserName Nome dell'altro partecipante.
 * @property otherUserId ID dell'altro partecipante.
 */
data class ChatPreview (
    val id: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,
    val otherUserPhoto: String? = null,
    val otherUserName: String = "",
    val otherUserId: String = ""
)
