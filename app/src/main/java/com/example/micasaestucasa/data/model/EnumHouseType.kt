package com.example.micasaestucasa.data.model

/**
 * Rappresenta le diverse tipologie di alloggio disponibili nell'applicazione.
 *
 * Questa enum viene utilizzata per catalogare le case e facilitare i filtri di ricerca.
 * Ogni tipologia è associata a un ID numerico per il database e a una stringa leggibile per l'utente.
 *
 * @property id Il codice numerico univoco salvato nel database.
 * @property label Il nome della tipologia in formato testo, da mostrare nell'interfaccia utente.
 */
enum class EnumHouseType(
    val id: Int,
    val label: String
) {
    /** Una casa indipendente o terratetto. */
    CASA(1, "Casa"),

    /** Una proprietà di lusso con ampi spazi o giardino. */
    VILLA(2, "Villa"),

    /** Un alloggio inserito in un condominio o palazzo. */
    APPARTAMENTO(3, "Appartamento"),

    /** Una stanza singola o doppia all'interno di un alloggio condiviso. */
    STANZA(4, "Stanza"),

    /** Un ampio spazio aperto, spesso ricavato da un ambiente industriale. */
    LOFT(5, "Loft"),

    /** Una struttura rustica, tipicamente in legno o immersa nella natura. */
    CABINA(6, "Cabina"),

    /** Qualsiasi altra tipologia di alloggio non presente nell'elenco. */
    ALTRO(7, "Altro");

    /**
     * Contiene i metodi di utilità per cercare e convertire le tipologie di alloggio.
     */
    companion object {

        /**
         * Cerca la tipologia di alloggio partendo dal suo nome testuale.
         *
         * @param label Il nome della tipologia da cercare (es. "Villa").
         * @return L'elemento [EnumHouseType] corrispondente, oppure `null` se il testo non esiste.
         */
        fun fromLabel(label: String): EnumHouseType? {
            return entries.find { it.label == label }
        }

        /**
         * Cerca la tipologia di alloggio partendo dal suo ID numerico.
         *
         * @param id L'identificativo numerico da cercare (es. 2).
         * @return L'elemento [EnumHouseType] corrispondente, oppure `null` se l'ID non esiste.
         */
        fun fromId(id: Int): EnumHouseType? {
            return entries.find { it.id == id }
        }

        fun fromValue(enumHouseType: EnumHouseType): String{
            return when(enumHouseType){
                CASA -> "Casa"
                VILLA -> "Villa"
                APPARTAMENTO -> "Appartamento"
                STANZA -> "Stanza"
                LOFT -> "Loft"
                CABINA -> "Cabina"
                ALTRO -> "Altro"
            }
        }

        fun fromString(enumHouseType: String): EnumHouseType {
            return when(enumHouseType){
                "Casa" -> CASA
                "Villa" -> VILLA
                "Appartamento" -> APPARTAMENTO
                "Stanza" -> STANZA
                "Loft" -> LOFT
                "Cabina" -> CABINA
                else -> ALTRO
            }
        }

        /**
         * Restituisce l'elenco completo dei nomi di tutte le tipologie disponibili.
         *
         * Utile per popolare i menu a tendina (Spinner) o i filtri nella schermata di ricerca.
         *
         * @return Una lista di stringhe contenente tutte le [label].
         */
        fun listEnumHouse(): List<String> {
            return entries.map { it.label }
        }
    }
}


