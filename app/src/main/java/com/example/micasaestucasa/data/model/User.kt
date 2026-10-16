package com.example.micasaestucasa.data.model

/**
 * Rappresenta un utente all'interno dell'applicazione.
 *
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
    val suspendedUntil: Long = 0,
    val accountStatus: String = "active",
    val fcmToken: String = "",
    val ruolo: String = "client",
    val numeroSoggiorni: Int = 0,
    val numeroRecensioni: Int = 0,
    val valutazioneMedia: Double = 0.0,
    val numeroAnnunci: Int = 0
){

    val badges: List<String>
        get(){
            val badgeList = mutableListOf<String>()

            if(valutazioneMedia>= 4.5 && numeroRecensioni >= 0)
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

    fun isRestricted(): Boolean {
        val now = System.currentTimeMillis()
        return accountStatus == "blocked" || (accountStatus == "suspended" && now < suspendedUntil)
    }


    fun getStatusMessage(): String? {
        val now = System.currentTimeMillis()
        return when (accountStatus) {
            "blocked" -> "Account bloccato definitivamente."
            "suspended" if now < suspendedUntil -> {
                val diff = suspendedUntil - now
                val days = diff / (24 * 60 * 60 * 1000)
                "Account sospeso per ancora $days giorni."
            }
            else -> null
        }
    }



}






