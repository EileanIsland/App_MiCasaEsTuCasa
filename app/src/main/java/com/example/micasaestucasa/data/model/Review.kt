package com.example.micasaestucasa.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Rappresenta una recensione all'interno dell'applicazione.
 * @property id l'ID univoco della recensione.
 * @property reviewerId l'ID dell'utente che ha scritto la recensione.
 * @property targetId l'ID dell'elemento recensito (casa o utente).
 * @property targetType il tipo di elemento recensito (casa o utente).
 * @property testo il testo della recensione.
 * @property timestamp la data della recensione.
 * @property reviewerName il nome dell'utente che ha scritto la recensione.
 * @property reviewerImageUrl l'URL dell'immagine dell'utente che ha scritto la recensione.
 * @property rating la valutazione finale della recensione, tra 0 e 5.
 */

data class Review(
    val id: String = "",
    val reviewerId: String = "",
    val targetId: String = "",

    // Mappiamo il targetType come Stringa per Firestore
    @get:PropertyName("targetType")
    @set:PropertyName("targetType")
    var targetTypeString: String = ReviewTarget.UTENTE.name, // Valore di default fondamentale!

    val rating: Float = 0f,
    val testo: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    val reviewerName: String = "",
    val reviewerImageUrl: String = ""
) {
    // Proprietà calcolata per usare l'Enum nel codice Kotlin
    @get:Exclude
    val targetType: ReviewTarget
        get() = try {
            ReviewTarget.valueOf(targetTypeString)
        } catch (e: Exception) {
            ReviewTarget.UTENTE
        }
}

