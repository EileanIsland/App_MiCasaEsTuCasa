package com.example.micasaestucasa.data.model

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
 * @property stato lo stato della prenotazione (ad esempio, "in attesa", "accettata", "rifiutata").
 */
data class Booking (
    val idBooking: String = "",
    val idCasa: String = "",
    val idUtente: String = "",
    val idHost: String = "",
    var dataInizio: String = "",
    var dataFine: String = "",
    var prezzoTotale: Double = 0.0,
    var stato: String = ""

)