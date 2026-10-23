package com.example.micasaestucasa.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Rappresenta una recensione all'interno dell'applicazione.
 *
 * @property id l'ID univoco della recensione.
 * @property reviewerId l'ID dell'utente che ha scritto la recensione.
 * @property targetId l'ID dell'elemento recensito (casa o utente).
 * @property targetTypeString Rappresentazione testuale del tipo di target per Firestore.
 * @property bookingId ID della prenotazione a cui si riferisce la recensione.
 * @property rating Valutazione numerica assegnata (es. da 0 a 5).
 * @property testo Il contenuto testuale della recensione.
 * @property timestamp Data e ora in cui è stata creata la recensione.
 * @property reviewerName Nome dell'utente che ha scritto la recensione.
 * @property reviewerImageUrl URL dell'immagine del profilo dell'autore della recensione.
 */
data class Review(
    val id: String = "",
    val reviewerId: String = "",
    val targetId: String = "",

    // Mappiamo il targetType come Stringa per Firestore
    @get:PropertyName("targetType")
    @set:PropertyName("targetType")
    var targetTypeString: String = ReviewTarget.UTENTE.name,

    val bookingId: String = "",
    val rating: Float = 0f,
    val testo: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    val reviewerName: String = "",
    val reviewerImageUrl: String = ""
) {
    /**
     * Proprietà calcolata per gestire il tipo di target tramite enum nel codice.
     */
    @get:Exclude
    val targetType: ReviewTarget
        get() = try {
            ReviewTarget.valueOf(targetTypeString)
        } catch (e: Exception) {
            ReviewTarget.UTENTE
        }
}



