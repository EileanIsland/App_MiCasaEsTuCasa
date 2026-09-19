package com.example.micasaestucasa.data.model

/**
 * Rappresenta una casa all'interno dell'applicazione.
 *
 *  Questa classe contiene tutte le informazioni relative alla casa:
 *  -le informazioni generali,
 *  -proprietario,
 *  -localizzazione,
 *  -immagini,
 *  -valutazione,
 *  -prezzo e diponibilità.
 *
 * @property id l'ID univoco della casa.
 * @property titolo il titolo della casa.
 * @property descrizione una breve descrizione della casa.
 * @property proprietarioId l'ID dell'utente proprietario della casa.
 * @property indirizzo l'indirizzo della casa.
 * @property citta la città in cui si trova la casa.
 * @property latitudine la latitudine della posizione geografica della casa.
 * @property longitudine la longitudine della posizione geografica della casa.
 * @property tipo il tipo di casa (es. "Villa", "Appartamento", "Casa").
 * @property ospitiMassimi il numero massimo di ospiti consentiti nella casa.
 * @property numeroCamere il numero di stanze presenti nella casa.
 * @property numeroLetti il numero di letti presenti nella casa.
 * @property numeroBagni il numero di bagni presenti nella casa.
 * @property prezzoNotte il prezzo della notte per la casa.
 * @property immagini una lista di URL delle immagini della casa.
 * @property esperienza una lista di tag associati all'esperienza della casa.
 * @property servizi una lista di tag associati ai servizi offerti dalla casa.
 * @property regole una lista di tag associati alle regole della casa.
 * @property valutazioneMedia la valutazione media della casa.
 * @property disponibilita una lista di date di disponibilità per la casa.
 * @property stato indica se la casa è ancora attiva sul mercato (true) o è stata cancellata (false).
 *
 */

data class Casa(

    val id: String,

    val titolo: String,
    val descrizione: String,

    val proprietarioId: String,

    val indirizzo: String,
    val citta: String,

    val latitudine: Double,
    val longitudine: Double,

    val tipo:  EnumHouseType = EnumHouseType.CASA,

    val ospitiMassimi: Int,
    val numeroCamere: Int,
    val numeroLetti: Int,
    val numeroBagni: Int,

    val prezzoNotte: Double,

    val immagini: List<String> = emptyList(),
    val esperienza: List<String> = emptyList(),
    val servizi: List<String> = emptyList(),
    val regole: List<String> = emptyList(),

    val valutazioneMedia: Double = 0.0,

    val disponibilita: List<Disponibilita> = emptyList(),
    var stato: Boolean = true //true se è una casa ancora attiva sul mercato, il suo annuncio non è stato cancellato
)


