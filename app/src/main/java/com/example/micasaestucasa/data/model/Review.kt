package com.example.micasaestucasa.data.model

/**
 * Rappresenta una recensione all'interno dell'applicazione.
 * @property id l'ID univoco della recensione.
 * @property reviewerId l'ID dell'utente che ha scritto la recensione.
 * @property targetId l'ID dell'elemento recensito (casa o utente).
 * @property targetType il tipo di elemento recensito (casa o utente).
 * @property initialRating la valutazione iniziale della recensione.
 * @property testo il testo della recensione.
 * @property date la data della recensione.
 * @property reviewerName il nome dell'utente che ha scritto la recensione.
 * @property reviewerImageUrl l'URL dell'immagine dell'utente che ha scritto la recensione.
 * @property rating la valutazione finale della recensione, tra 0 e 5.
 */
data class Review(

    val id: String = "",
    val reviewerId: String = "",    //utente che ha scritto la recensione
    val targetId: String = "", // elemento recensito (casa o utente)
    val targetType: ReviewTarget, // tipo di elemento recensito (casa o utente)
    val initialRating: Int = 0,    // dati recensione
    val testo: String = "",
    val date: Long = 0L,

    // dati recuperati dall'utente
    val reviewerName: String = "",
    val reviewerImageUrl: String = ""
){
    val rating: Int = initialRating.coerceIn(0, 5)

}

