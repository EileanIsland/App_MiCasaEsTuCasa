package com.example.micasaestucasa.data.model

/**
 * Rappresenta un tag associato a una casa (es. "WiFi", "No Animali").
 *
 * @property nome Il nome identificativo del tag.
 * @property categoria La categoria di appartenenza del tag (Servizio, Regola, Esperienza).
 */
data class Tag(
    val nome: String = "",
    val categoria: TagCategory

)


