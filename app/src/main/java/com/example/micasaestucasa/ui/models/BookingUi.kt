package com.example.micasaestucasa.ui.models

import com.example.micasaestucasa.data.model.Booking
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.data.model.User

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