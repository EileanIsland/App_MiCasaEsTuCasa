package com.example.micasaestucasa.data.model


//WORK in PROGRESS -
/**
 * Rappresenta un intervallo di tempo all'interno dell'applicazione.
 *
 * @property inizio il tempo di inizio dell'intervallo.
 * @property fine il tempo di fine dell'intervallo.
 */
data class Disponibilita (
    val inizio: Long = 0L,
    val fine: Long = 0L
)