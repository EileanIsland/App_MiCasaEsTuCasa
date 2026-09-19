package com.example.micasaestucasa.data.model

/**
 * Rappresenta un utente all'interno dell'applicazione.
 * @property id l'ID univoco dell'utente.
 * @property name il nome dell'utente.
 * @property surname il cognome dell'utente.
 * @property email l'indirizzo email dell'utente.
 * @property phone il numero di telefono dell'utente.
 * @property bio una breve descrizione dell'utente.
 * @property profileImageUrl l'URL dell'immagine di profilo dell'utente.
 * @property stato indica se l'utente è attivo o meno.
 * @property badge una lista di tag associati al badge dell'utente.
 * @property ruolo indica il ruolo dell'utente nella piattaforma.
 *
 */
data class User(
    val id: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "Nessuna bio",
    val profileImageUrl: String = "",
    val stato: Boolean = true, //indica se utente attivo

    val badge : List<String> = emptyList(),
    val ruolo: String = "client"

)



// riferimenti alle case pubblicate dall'utente
//val publishedHouseIds: List<String> = emptyList(),

// riferimenti alle prenotazioni effettuate
//val bookingIds: List<String> = emptyList(),

//val receivedBookingIds: List<String> = emptyList(),

// recensioni scritte
//val writtenReviewIds: List<String> = emptyList(),

//recensioni ricevute
//val receivedReviewIds: List<String> = emptyList(),

