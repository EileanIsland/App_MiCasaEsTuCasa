package com.example.micasaestucasa.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Rappresenta la prenotazione di una casa all'interno dell'applicazione.
 *
 * Questa classe contiene tutte le informazioni relative al soggiorno prenotato da un utente,
 * inclusi i dettagli su ospite e host, le date e i costi.
 *
 * @property idBooking l'ID univoco della prenotazione.
 * @property idCasa l'ID della casa prenotata.
 * @property idUtente l'ID dell'utente che ha prenotato la casa.
 * @property idHost l'ID dell'host della casa.
 * @property dataInizio la data di inizio del stayed.
 * @property dataFine la data di fine del stayed.
 * @property prezzoTotale il prezzo totale della prenotazione.
 * @property numeroOspiti il numero di ospiti per la prenotazione.
 * @property stato lo stato della prenotazione (ad esempio, "in attesa", "accettata", "rifiutata").
 * @property url l'URL dell'immagine associata alla casa.
 * @property titoloCasa il titolo della casa prenotata.
 * @property isHouseReviewed un flag che indica se la casa è stata recensita.
 * @property isUserReviewed un flag che indica se l'utente è stato recensito.
 */
data class Booking (
    val idBooking: String = "",
    val idCasa: String = "",
    val idUtente: String = "",
    val idHost: String = "",
    var dataInizio: Long = 0,
    var dataFine: Long = 0,
    var prezzoTotale: Double = 0.0,
    var numeroOspiti: Int = 1,
    var stato: String = "", //In attesa, Confermata, Rifiutata, Cancellata
    var url: String? = null,
    var titoloCasa: String? = null,

    @get:PropertyName("isHouseReviewed")
    @set:PropertyName("isHouseReviewed")
    var isHouseReviewed : Boolean = false,

    @get:PropertyName("isUserReviewed")
    @set:PropertyName("isUserReviewed")
    var isUserReviewed: Boolean = false

){
    /**
     * Restituisce una descrizione testuale dello stato della prenotazione per l'UI.
     * @return Stringa descrittiva dello stato attuale.
     */
    fun getDisplayStatus(): String {
        val now = System.currentTimeMillis()

        return when (stato) {
            "Cancellata" -> "Cancellata"
            "Rifiutata" -> "Rifiutata"
            "In attesa" -> "In attesa"
            "Confermata" -> {
                when {
                    now < dataInizio -> "Confermata (In arrivo)"
                    now in dataInizio..dataFine -> "Soggiorno in corso"
                    else -> "Conclusa"
                }
            }
            else -> stato
        }
    }


    /**
     * Verifica se il soggiorno è terminato o se la prenotazione è stata rifiutata.
     */
    private fun isTimeExpired(): Boolean {
        val now = System.currentTimeMillis()
        return ((stato == "Confermata" || stato == "Conclusa") && (now > dataFine)) || (stato == "Rifiutata")
    }

    /**
     * Verifica se l'ospite può lasciare una recensione per la casa.
     * @param isHost Indica se l'utente corrente è l'host.
     * @return True se l'ospite può recensire, false altrimenti.
     */
    fun canReviewHouse(isHost: Boolean): Boolean {
        if (isHost) return false
        if (stato == "Cancellata") return false

        return isTimeExpired() && !isHouseReviewed
    }

    /**
     * Verifica se l'host può lasciare una recensione per l'ospite.
     * @return True se l'host può recensire, false altrimenti.
     */
    fun canReviewUser(): Boolean {
        return isTimeExpired() && !isUserReviewed
    }
}




