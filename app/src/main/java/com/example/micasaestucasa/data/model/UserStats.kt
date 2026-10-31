package com.example.micasaestucasa.data.model

/**
 * Rappresenta le statistiche di un utente all'interno dell'applicazione.
 * @property ratingMedia Media delle valutazioni ricevute.
 * @property numeroRecensioni Numero totale di recensioni ricevute o effettuate.
 * @property numeroSoggiorni Numero totale di soggiorni effettuati.
 * @property numeroAnnunci Numero di annunci pubblicati dall'utente.
 */
data class UserStats (
    val ratingMedia: Double ? = 0.0,
    val numeroRecensioni: Int? =0,
    val numeroSoggiorni: Int? = 0,
    val numeroAnnunci: Int? = 0
)