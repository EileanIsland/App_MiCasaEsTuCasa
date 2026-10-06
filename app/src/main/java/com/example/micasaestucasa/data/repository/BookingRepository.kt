package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookingCollection = db.collection("bookings")

    /**
     * Recupera tutte le prenotazioni effettuate da un utente specifico (Ospite)
     */
    suspend fun getBookingByGuest(guestId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingCollection
                .whereEqualTo("idUtente", guestId)
                // Se vuoi mostrare solo quelle accettate, mantieni il filtro,
                // altrimenti toglilo per mostrare anche quelle in attesa/rifiutate
                .get()
                .await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera tutte le prenotazioni ricevute da un proprietario (Host)
     */
    suspend fun getBookingByHost(hostId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingCollection
                .whereEqualTo("idHost", hostId)
                .get()
                .await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Salva o aggiorna i dati di una prenotazione.
     * Restituisce l'ID della prenotazione in caso di successo.
     */
    suspend fun saveBooking(booking: Booking): Result<String> {
        return try {
            val docRef = if (booking.idBooking.isEmpty()) {
                bookingCollection.document()
            } else {
                bookingCollection.document(booking.idBooking)
            }
            val finalBooking = booking.copy(idBooking = docRef.id)
            docRef.set(finalBooking).await()
            Result.success(finalBooking.idBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna lo stato di una prenotazione (es: Accettata, Rifiutata)
     */
    //TODO: CAMBIARE CON ACCEPT BOOKING e CON REFUSE BOOKING

    suspend fun updateBookingStatus(bookingId: String, newStatus: String): Result<Unit> {
        return try {
            bookingCollection.document(bookingId).update("stato", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Accetta una prenotazione:
     * 1. Recupera la prenotazione e la casa.
     * 2. Verifica che le date siano ancora disponibili.
     * 3. "Spacca" l'intervallo di disponibilità della casa.
     * 4. Aggiorna entrambi i documenti.
     */
    suspend fun acceptBooking(bookingId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val bookingRef = bookingCollection.document(bookingId)
                val bookingSnapshot = transaction.get(bookingRef)
                val booking = bookingSnapshot.toObject(Booking::class.java) ?: throw Exception("Prenotazione non trovata")

                if (booking.stato != "In attesa" && booking.stato != "Pending") {
                    throw Exception("La prenotazione non è più in attesa.")
                }

                val houseRef = db.collection("case").document(booking.idCasa)
                val houseSnapshot = transaction.get(houseRef)

                val currentDisponibilita = houseSnapshot.get("disponibilita") as? List<Map<String, Long>> ?: emptyList()

                val startB = booking.dataInizio
                val endB = booking.dataFine

                val matchingInterval = currentDisponibilita.find {
                    val inizioA = it["inizio"] ?: 0L
                    val fineA = it["fine"] ?: 0L
                    startB >= inizioA && endB <= fineA
                } ?: throw Exception("Le date selezionate non sono più disponibili.")

                val inizioA = matchingInterval["inizio"] ?: 0L
                val fineA = matchingInterval["fine"] ?: 0L

                val newAvailableList = currentDisponibilita.toMutableList()
                newAvailableList.remove(matchingInterval)

                if (inizioA < startB) {
                    newAvailableList.add(mapOf("inizio" to inizioA, "fine" to startB))
                }
                if (fineA > endB) {
                    newAvailableList.add(mapOf("inizio" to endB, "fine" to fineA))
                }

                // 4. ESECUZIONE AGGIORNAMENTI
                transaction.update(bookingRef, "stato", "Confermata")
                transaction.update(houseRef, "disponibilita", newAvailableList)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun refuseBooking(bookingId: String): Result<Unit> {
        return try {
            bookingCollection.document(bookingId).update("stato", "Rifiutata").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera i dettagli di una singola prenotazione tramite ID
     */
    suspend fun getBookingById(bookingId: String): Result<Booking?> {
        return try {
            val snapshot = bookingCollection.document(bookingId).get().await()
            val booking = snapshot.toObject(Booking::class.java)
            if (booking != null) {
                Result.success(booking)
            } else {
                Result.failure(Exception("Prenotazione non trovata"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina una prenotazione dallo storage di Firestore
     */
    suspend fun deleteBooking(bookingId: String): Result<Unit> {
        return try {
            bookingCollection.document(bookingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Conta il numero di prenotazioni effettuate da un utente
     */
    suspend fun getBookingCountByGuest(guestId: String): Result<Int> {
        return try {
            val snapshot = bookingCollection
                .whereEqualTo("idUtente", guestId)
                .whereEqualTo("stato", "accepted")
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

