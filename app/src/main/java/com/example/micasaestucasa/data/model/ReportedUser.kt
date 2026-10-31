package com.example.micasaestucasa.data.model

/**
 * Rappresenta un utente segnalato per un comportamento scorretto.
 * @property user l'utente segnalato.
 * @property reports le segnalazioni ricevute da quell'utente.
 * @property isExpanded un flag che indica se la lista delle segnalazioni è espansa (per fragment).
 */
data class ReportedUser(
    val user: User,
    val reports: List<Report> = emptyList(),
    var isExpanded: Boolean = false
)

