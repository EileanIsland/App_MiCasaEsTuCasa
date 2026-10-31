package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Casa
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await


object CasaRepository {
    private val db get() = FirebaseFirestore.getInstance()
    private val caseCollection = db.collection("case")


    // Recupera tutte le case
    suspend fun getAllCase(): Result<List<Casa>> {
        return try {
            android.util.Log.d("FIRESTORE_GET_ALL_CASE", "getAllCase() - tentativo recupero case da Firestore...")
            val snapshot = caseCollection.whereEqualTo("stato", true).get().await()

            android.util.Log.d("FIRESTORE_GET_ALL_CASE", "Documenti trovati: ${snapshot.size()}")

            val listaCase = snapshot.toObjects(Casa::class.java)
            android.util.Log.d("FIRESTORE_GET", "Conversione riuscita! Case convertite: ${listaCase.size}")
            Result.success(listaCase)
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE_GET", "!!! ERRORE CRITICO NEL RECUPERO CASE !!!", e)
            Result.failure(e)
        }
    }


    // Recupera casa tramite Id
    suspend fun getCasaById(id: String): Result<Casa?> {
        return try {
            val snapshot = caseCollection.document(id).get().await()
            android.util.Log.d("FIRESTORE_DEBUG", "Data: ${snapshot.data}")
            Result.success(snapshot.toObject(Casa::class.java))
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE_ERROR", "Error: ${e.message}")
            Result.failure(e)
        }
    }


    // Salva casa
    suspend fun saveCasa(casa: Casa): Result<String> {
        return try {
            val isNew = casa.id.isEmpty()
            val docRef = if (isNew) caseCollection.document() else caseCollection.document(casa.id)
            val casaFinal = casa.copy(id = docRef.id)

            db.runTransaction { transaction ->
                transaction.set(docRef, casaFinal)

                if (isNew) {
                    val ownerRef = db.collection("users").document(casa.proprietarioId)
                    transaction.update(ownerRef, "numeroAnnunci", FieldValue.increment(1))
                }
            }.await()
            Result.success(casaFinal.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Rimuove casa
    suspend fun removeCasa(casa: Casa): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                transaction.delete(caseCollection.document(casa.id))

                val ownerRef = db.collection("users").document(casa.proprietarioId)
                transaction.update(ownerRef, "numeroAnnunci", FieldValue.increment(-1))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }


    // Disattiva casa
    suspend fun deactivateCasa(casaId: String): Result<Unit> {
        return try {
            caseCollection.document(casaId)
                .update("stato", false)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Case per proprietario
    suspend fun getCaseByProprietario(ownerId: String): Result<List<Casa>> {
        return try {
            val snapshot = caseCollection.whereEqualTo("proprietarioId", ownerId)
                .whereEqualTo("stato", true).get().await()
            Result.success(snapshot.toObjects(Casa::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Case con valutazione più alta
    suspend fun getTopRatedHouses(): Result<List<Casa>> {
        return try {
            val snapshot = caseCollection.orderBy("valutazioneMedia", Query.Direction.DESCENDING).limit(5).get().await()
            Result.success(snapshot.toObjects(Casa::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Conteggio case per proprietario
    suspend fun getCountHousesByOwner(ownerId: String): Result<Int> {
        return try {
            val snapshot = caseCollection.whereEqualTo("proprietarioId", ownerId).get().await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
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
    ): Result<List<Casa>> {
        return try {
            val allHouses = getAllCase().getOrElse { emptyList() }

            val minPrice = priceRange.getOrNull(0) ?: 0
            val maxPrice = priceRange.getOrNull(1) ?: 10000

            if (query.isEmpty() && numPerson <= 0 && date.isEmpty() && category.isEmpty() &&
                services.isEmpty() && experiences.isEmpty() &&
                numBathroom == 0 && numBeds == 0 && minPrice <= 0 && maxPrice >= 1000
            ) {
                android.util.Log.d("FIRESTORE_SEARCH", "Nessun filtro applicato. Restituisco tutte le ${allHouses.size} case.")
                return Result.success(allHouses)
            }

            val filtered = allHouses.filter { casa ->
                val matchesQuery = query.isEmpty() || (
                        casa.titolo.contains(query, ignoreCase = true) ||
                                casa.descrizione.contains(query, ignoreCase = true) ||
                                casa.citta.contains(query, ignoreCase = true) ||
                                casa.tipo.label.contains(query, ignoreCase = true) ||
                                casa.servizi.any { it.contains(query, ignoreCase = true) } ||
                                casa.esperienza.any { it.contains(query, ignoreCase = true) }
                        )

                val matchesCategory = category.isEmpty() || casa.tipo.label.equals(category, ignoreCase = true)

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

                matchesQuery && matchesDate && matchesPrices &&
                        matchesCategory &&
                        matchesCapacity && matchesBeds && matchesBathrooms &&
                        matchesServices && matchesExperiences
            }

            android.util.Log.d("SEARCH_DEBUG", "Case trovate: ${filtered.size} su ${allHouses.size} - Query: $query")
            Result.success(filtered.sortedByDescending { it.valutazioneMedia })
        } catch (e: Exception) {
            android.util.Log.e("SEARCH_DEBUG", "Errore durante il filtraggio", e)
            Result.failure(e)
        }
    }
}


