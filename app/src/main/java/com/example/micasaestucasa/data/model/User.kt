package com.example.micasaestucasa.data.model

/**
 * Rappresenta un utente all'interno dell'applicazione.
 *
 * @property id Identificativo univoco dell'utente.
 * @property name Nome dell'utente.
 * @property surname Cognome dell'utente.
 * @property email Indirizzo email dell'utente.
 * @property phone Numero di telefono dell'utente.
 * @property bio Breve descrizione biografica dell'utente.
 * @property profileImageUrl URL dell'immagine del profilo.
 * @property suspendedUntil Timestamp fino al quale l'account è sospeso.
 * @property accountStatus Stato dell'account (es. "active", "blocked", "suspended").
 * @property ruolo Ruolo dell'utente (es. "client", "admin").
 * @property numeroSoggiorni Numero totale di soggiorni effettuati.
 * @property numeroRecensioni Numero totale di recensioni ricevute o effettuate.
 * @property valutazioneMedia Media delle valutazioni ricevute.
 * @property numeroAnnunci Numero di annunci pubblicati dall'utente.
 */
data class User(
    val id: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "Nessuna bio",
    val profileImageUrl: String = "",
    val suspendedUntil: Long = 0,
    val accountStatus: String = "active",
    val ruolo: String = "client",
    val numeroSoggiorni: Int = 0,
    val numeroRecensioni: Int = 0,
    val valutazioneMedia: Double = 0.0,
    val numeroAnnunci: Int = 0
){

    /**
     * Calcola e restituisce la lista dei badge assegnati all'utente in base alle sue statistiche.
     */
    val badges: List<String>
        get(){
            val badgeList = mutableListOf<String>()

            if(valutazioneMedia>= 4.5)
                badgeList.add("Top Rated")

            if (numeroAnnunci > 0 && valutazioneMedia >= 4.8 && numeroRecensioni >= 5)
                badgeList.add("Super Host")

            //almeno 3 soggiorni
            if (numeroSoggiorni >= 3)
                badgeList.add("Frequent Traveller")

            if(ruolo=="admin"){
                badgeList.add("Amministrator")
            }

            //almeno 3 case pubblicate
            if (numeroAnnunci >= 3)
                badgeList.add("Experienced Host")

            return badgeList
        }

    /**
     * Verifica se l'utente ha restrizioni all'accesso (bloccato o sospeso).
     * @return True se l'account è bloccato o sospeso, false altrimenti.
     */
    fun isRestricted(): Boolean {
        val now = System.currentTimeMillis()
        return accountStatus == "blocked" || (accountStatus == "suspended" && now < suspendedUntil)
    }


    /**
     * Restituisce un messaggio descrittivo dello stato dell'account se ristretto.
     * @return Il messaggio di stato o null se l'account non è ristretto.
     */
    fun getStatusMessage(): String? {
        val now = System.currentTimeMillis()
        return when (accountStatus) {
            "blocked" -> "Account bloccato definitivamente."
            "suspended" if now < suspendedUntil -> {
                val diff = suspendedUntil - now
                val days = diff / (24 * 60 * 60 * 1000)
                "Account sospeso per ancora ${days+1} giorni."
            }
            else -> null
        }
    }



}






