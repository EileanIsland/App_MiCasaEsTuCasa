package com.example.micasaestucasa.data.model

/**
 * Rappresenta una categoria di tag (es. "Servizi", "Regole", "Esperienza").
 * @property nome Il nome della categoria.
 * @property categoria una breve descrizione della categoria.
 */
data class Tag(
    val nome: String = "",
    val categoria: TagCategory

)



/*
idee tag per la ricerca


//SERVIZI
Wi-Fi
Parcheggio
Aria condizionata
Riscaldamento
Lavatrice
Lavastoviglie
Cucina attrezzata
TV
Ascensore

//REGOLE
Animali ammessi
Adatto a famiglie
Accessibile disabili
Vietato fumare
Self check-in

//Esoerienza
Romantico
Pet friendly
Business
Adatto a gruppi
Ideale per coppie
Vicino ai mezzi pubblici

 */
