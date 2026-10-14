package com.example.micasaestucasa.data.model

/**
 * Rappresenta un utente all'interno dell'applicazione.
 * @property id l'ID univoco dell'utente.
 * @property name il nome dell'utente.
 * @property surname il cognome dell'utente.
 * @property email l'indirizzo email dell'utente.
 * @property phone il numero di telefono dell'utente.
 * @property bio una breve descrizione dell'utente.
 * @property profileImageUrl l'URL dell'immagine di profilo dell'utente.
 * @property stato indica se l'utente è attivo o meno.
 * @property badge una lista di tag associati al badge dell'utente.
 * @property ruolo indica il ruolo dell'utente nella piattaforma.
 *
 */
data class User(
    val id: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "Nessuna bio",
    val profileImageUrl: String = "",
    val stato: Boolean = true, //indica se utente attivo
    val ruolo: String = "client",
    val numeroSoggiorni: Int = 0,
    val numeroRecensioni: Int = 0,
    val valutazioneMedia: Double = 0.0,
    val numeroAnnunci: Int = 0
){

    val badges: List<String>
        get(){

            val badgeList = mutableListOf<String>()

            if(valutazioneMedia>= 4.5 && numeroRecensioni >= 0){
                badgeList.add("Top Rated")
            }

            if (numeroAnnunci > 0 && valutazioneMedia >= 4.8 && numeroRecensioni >= 5) {
                badgeList.add("Super Host")
            }

            //almeno 3 soggiorni
            if (numeroSoggiorni >= 3) {
                badgeList.add("Frequent Traveller")
            }

            //almeno 3 case pubblicate
            if (numeroAnnunci >= 3) {
                badgeList.add("Experienced Host")
            }

            return badgeList
        }

}






