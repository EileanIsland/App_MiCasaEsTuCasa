package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Casa
import com.google.firebase.firestore.FirebaseFirestore.getInstance
import kotlinx.coroutines.tasks.await


object CasaRepository {

    private val db = getInstance()
    private val caseCollection = db.collection("case")

    //recupera tutte le case
    suspend fun getAllCase(): List<Casa> {
        return try {
            android.util.Log.d("FIRESTORE_GET_ALL_CASE", "getAllCase() - tentativo recupero case da Firestore...")
            val snapshot = caseCollection.get().await()

            android.util.Log.d("FIRESTORE_GET_ALL_CASE", "Documenti trovati: ${snapshot.size()}")

            val listaCase = snapshot.toObjects(Casa::class.java)
            android.util.Log.d("FIRESTORE_GET", "Conversione riuscita! Case convertite: ${listaCase.size}")
            listaCase

        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE_GET", "!!! ERRORE CRITICO NEL RECUPERO CASE !!!", e)
            emptyList()
        }
    }

    //recupera casa tramite Id
    suspend fun getCasaById(id: String): Casa? {
        return try {
            val snapshot = caseCollection.document(id).get().await()
            snapshot.toObject(Casa::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveCasa(casa: Casa): String {
        return try {
            val docRef = if (casa.id.isEmpty()) {
                caseCollection.document()
            } else {
                caseCollection.document(casa.id)
            }
            val casaFinal = casa.copy(id = docRef.id)
            docRef.set(casaFinal).await()
            casaFinal.id
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun removeCasa(id: String) {
        try {
            caseCollection.document(id).delete().await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getCaseByProprietario(ownerId: String): List<Casa> {
        return try {
            val snapshot = caseCollection.whereEqualTo("proprietarioId", ownerId).get().await()
            snapshot.toObjects(Casa::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }


    suspend fun getTopRatedHouses(): List<Casa> {
        return try {                                                         //ne prende 5
            val snapshot = caseCollection.orderBy("valutazioneMedia").limit(5).get().await()
            snapshot.toObjects(Casa::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCountHousesByOwner(ownerId: String): Int {
        return try {
            val snapshot = caseCollection.whereEqualTo("proprietarioId", ownerId).get().await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }


    suspend fun searchHouses(
        query: String,
        numPerson: Int,
        date: List<Long>,
        services: List<String>,
        experiences: List<String>,
        priceRange: List<Int>,
        numBathroom: Int,
        numBeds: Int,
        category: String
    ): List<Casa> {
        return try {
            val allHouses = getAllCase()

            val minPrice = priceRange.getOrNull(0) ?: 0 //TODO: non dovrebbero essere double?
            val maxPrice = priceRange.getOrNull(1) ?: 10000

            // Se non ci sono filtri, restituisci tutto
            if (query.isEmpty() && numPerson <= 0 && date.isEmpty() &&
                services.isEmpty() && experiences.isEmpty() &&
                category.isEmpty() && numBathroom == 0 && numBeds == 0 && minPrice<= 0 && maxPrice >= 1000
            ) {
                android.util.Log.d("FIRESTORE_SEARCH", "Nessun filtro applicato. Restituisco tutte le ${allHouses.size} case.")
                return allHouses
            }

            val filtered = allHouses.filter { casa ->
                val matchesQuery = query.isEmpty() || (
                        casa.titolo.contains(query, ignoreCase = true) ||
                                casa.descrizione.contains(query, ignoreCase = true) ||
                                casa.citta.contains(query, ignoreCase = true)
                        )

                val matchesCategory = category.isEmpty() || (
                        casa.tipo.label.contains(category, ignoreCase = true) ||
                        casa.descrizione.contains(category, ignoreCase = true) ||
                        casa.esperienza.any{it.contains(category, ignoreCase = true)} ||
                        casa.regole.any{it.contains(category, ignoreCase = true)} ||
                        casa.servizi.any{it.contains(category, ignoreCase = true)}
                        )

                val matchesDate = if (date.size >= 2 && date[0] > 0 && date[1] > 0) {
                    casa.disponibilita.any { d ->
                        date[0] >= d.inizio && date[1] <= d.fine
                    }
                } else true

                val matchesPrices = casa.prezzoNotte >= minPrice && casa.prezzoNotte <= maxPrice
                val matchesCapacity = casa.ospitiMassimi >= numPerson
                val matchesBeds = casa.numeroLetti >= numBeds
                val matchesBathrooms = casa.numeroBagni >= numBathroom
                val matchesServices = services.isEmpty() || casa.servizi.containsAll(services)
                val matchesExperiences = experiences.isEmpty() || casa.esperienza.containsAll(experiences)

                matchesQuery && matchesCategory && matchesDate && matchesPrices &&
                        matchesCapacity && matchesBeds && matchesBathrooms &&
                        matchesServices && matchesExperiences
            }

            android.util.Log.d("SEARCH_DEBUG", "Case trovate: ${filtered.size} su ${allHouses.size}")

            filtered.sortedBy { it.valutazioneMedia }
        } catch (e: Exception) {
            android.util.Log.e("SEARCH_DEBUG", "Errore durante il filtraggio", e)
            emptyList()
        }
    }
}





    /*
    private val case = mutableListOf(
        Casa(
            id = "casa001",
            titolo = "Casa al mare",
            descrizione = "Appartamento luminoso vicino alla spiaggia con balcone vista mare.",
            proprietarioId = "user001",
            indirizzo = "Via Roma 25",
            citta = "Genova",
            latitudine = 44.4056,
            longitudine = 8.9463,
            tipo = EnumHouseType.APPARTAMENTO,
            ospitiMassimi = 4,
            numeroCamere = 2,
            numeroLetti = 3,
            numeroBagni = 1,
            prezzoNotte = 70.0,
            immagini = listOf(
                "https://example.com/mare1.jpg",
                "https://example.com/mare2.jpg"
            ),
            esperienza = listOf(
                "Vista mare",
                "Vicino alla spiaggia",
                "Ideale per famiglie"
            ),
            servizi = listOf(
                "Wi-Fi",
                "Balcone",
                "Cucina attrezzata"
            ),
            regole = listOf(
                "Animali ammessi",
                "Non fumatori"
            ),
            valutazioneMedia = 4.8,
            disponibilita = listOf(
                Disponibilita(
                    "2026-06-01".toLongDate(),
                    "2026-09-30".toLongDate()
                )
            ),
            recensioni = emptyList()
        ),

        Casa(
            id = "casa002",
            titolo = "Loft moderno in centro",

            descrizione =
                "Loft elegante nel centro città, perfetto per soggiorni brevi.",

            proprietarioId = "user002",

            indirizzo = "Via Mazzini 10",
            citta = "Torino",

            latitudine = 45.0703,
            longitudine = 7.6869,

            tipo = EnumHouseType.LOFT,

            ospitiMassimi = 2,
            numeroCamere = 1,
            numeroLetti = 1,
            numeroBagni = 1,

            prezzoNotte = 85.0,


            immagini = listOf(
                "https://example.com/loft1.jpg"
            ),


            esperienza = listOf(
                "Centro città",
                "Soggiorno romantico",
                "Ideale per coppie"
            ),


            servizi = listOf(
                "Wi-Fi",
                "Smart TV",
                "Self check-in"
            ),


            regole = listOf(
                "Non fumatori",
                "No feste"
            ),


            valutazioneMedia = 4.6,


            disponibilita = listOf(
                Disponibilita(
                    "2026-01-10".toLongDate(),
                    "2026-12-20".toLongDate()
                )
            ),
            recensioni = emptyList()
        ),

        Casa(
            id = "casa003",

            titolo = "Villa con piscina",

            descrizione =
                "Villa spaziosa con giardino privato e piscina.",

            proprietarioId = "user003",

            indirizzo = "Strada delle Colline 5",
            citta = "Asti",

            latitudine = 44.9000,
            longitudine = 8.2064,

            tipo = EnumHouseType.VILLA,

            ospitiMassimi = 8,
            numeroCamere = 4,
            numeroLetti = 6,
            numeroBagni = 3,

            prezzoNotte = 150.0,


            immagini = listOf(
                "https://example.com/villa1.jpg",
                "https://example.com/villa2.jpg"
            ),


            esperienza = listOf(
                "Vista panoramica",
                "Relax nella natura",
                "Ideale per gruppi"
            ),


            servizi = listOf(
                "Piscina",
                "Giardino privato",
                "Parcheggio",
                "Aria condizionata"
            ),


            regole = listOf(
                "Animali ammessi",
                "Non fumatori",
                "Rispetto del vicinato"
            ),


            valutazioneMedia = 4.9,


            disponibilita = listOf(
                Disponibilita(
                    "2026-04-01".toLongDate(),
                    "2026-10-31".toLongDate()
                )
            )
            ,
            recensioni = emptyList()

        )
    )
     */

