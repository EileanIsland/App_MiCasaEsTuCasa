package com.example.micasaestucasa.data.model


/**
 * Rappresenta una segnalazione inviata da un utente nei confronti di un altro.
 *
 * @property id Identificativo univoco della segnalazione.
 * @property reportedUserId ID dell'utente che è stato segnalato.
 * @property reporterUserId ID dell'utente che ha effettuato la segnalazione.
 * @property status Stato della segnalazione (es. "pending", "handled").
 * @property reporterName Nome dell'utente segnalatore.
 * @property reason Motivo della segnalazione.
 * @property timestamp Timestamp del momento in cui è stata effettuata la segnalazione.
 */
data class Report(
    val id: String = "",
    val reportedUserId: String = "",
    val reporterUserId: String = "",
    val status: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val timestamp: Long = 0
)
