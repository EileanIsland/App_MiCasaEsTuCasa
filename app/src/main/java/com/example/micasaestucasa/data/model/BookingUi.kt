package com.example.micasaestucasa.data.model

/**
 * Modello di supporto per gli adapter che gestiscono le prenotazioni,
 * collega le info della prenotazione
 * @property booking prenotazione
 * @property casa casa
 * @property owner (user) proprietario
 * @property guest (User) ospite
 */
data class BookingUi(
    val booking: Booking?,
    val casa: Casa?,
    val owner: User?,
    val guest: User?
)