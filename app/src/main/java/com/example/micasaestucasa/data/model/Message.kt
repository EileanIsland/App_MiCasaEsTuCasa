package com.example.micasaestucasa.data.model


/**
 * Rappresenta un singolo messaggio all'interno di una conversazione.
 *
 * @property id Identificativo univoco del messaggio.
 * @property text Contenuto testuale del messaggio.
 * @property url URL di un'eventuale immagine allegata al messaggio.
 * @property type Tipo di messaggio (es. "text", "image", "context", "booking_summary".
 * @property senderId ID dell'utente che ha inviato il messaggio.
 * @property timestamp Timestamp del momento dell'invio.
 * @property contextId ID dell'elemento correlato al messaggio (es. ID casa o ID prenotazione).
 * @property contextType Tipo di contesto correlato (es. "HOUSE", "BOOKING").
 * @property contextTitle Titolo o descrizione breve dell'elemento correlato.
 */
data class Message(
    val id: String = "",
    val text: String = "",
    val url: String? = null,
    val type: String = "text", // text or image or context
    val senderId: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    val contextId: String? = null,
    val contextType: String? = null, //HOUSE o BOOKING o BOOKING_SUMMARY
    val contextTitle: String? = null
)
