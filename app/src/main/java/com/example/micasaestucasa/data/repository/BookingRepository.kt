package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object BookingRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val bookingCollection get() = db.collection("bookings")
    private var bookingListener: com.google.firebase.firestore.ListenerRegistration? = null


    /**
     * Avvia l'ascolto delle prenotazioni per l'utente corrente.
     * Se lo stato cambia o viene aggiunta una prenotazione, invia una notifica locale.
     */
    fun startBookingListener(context: android.content.Context, userId: String) {
        bookingListener?.remove()

        bookingListener = bookingCollection
            .whereIn("idHost", listOf(userId))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.w("BookingRepo", "Listen failed.", e)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { dc ->
                    val booking = dc.document.toObject(Booking::class.java)

                    when (dc.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            // NOTIFICA PER HOST: Nuova prenotazione ricevuta
                            if (booking.idHost == userId && (booking.stato == "In attesa" || booking.stato == "Pending")) {
                                com.example.micasaestucasa.utils.NotificationHelper.sendBookingNotification(
                                    context,
                                    "Nuova Richiesta",
                                    "Hai ricevuto una nuova richiesta per: ${booking.titoloCasa}"
                                )
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            // NOTIFICA PER GUEST: Stato della prenotazione cambiato (Accettata/Rifiutata)
                            if (booking.idUtente == userId) {
                                val statusMessage = when (booking.stato) {
                                    "Confermata" -> "La tua prenotazione per ${booking.titoloCasa} è stata confermata!"
                                    "Rifiutata" -> "La tua richiesta per ${booking.titoloCasa} è stata rifiutata."
                                    else -> null
                                }

                                statusMessage?.let {
                                    com.example.micasaestucasa.utils.NotificationHelper.sendBookingNotification(
                                        context,
                                        "Aggiornamento Prenotazione",
                                        it
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
    }

    /**
     * Ferma il listener quando l'utente fa il logout o l'app viene chiusa
     */
    fun stopBookingListener() {
        bookingListener?.remove()
        bookingListener = null
    }


    /**
     * Recupera tutte le prenotazioni effettuate da un utente specifico (Ospite)
     */
    suspend fun getBookingByGuest(guestId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingCollection
                .whereEqualTo("idUtente", guestId)
                .get()
                .await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun markAsReviewed(bookingId: String): Result<Unit> {
        return try {
            val bookingRef = bookingCollection.document(bookingId)

            bookingRef.update("isReviewed", true).await()
            Result.success(Unit)
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
            val isNewBooking = booking.idBooking.isEmpty()
            val docRef = if (isNewBooking) {
                bookingCollection.document()
            } else {
                bookingCollection.document(booking.idBooking)
            }

            val finalBooking = booking.copy(idBooking = docRef.id)
            docRef.set(finalBooking).await()

            if(isNewBooking){
                val messageResult = sendBookingSystemMessage(finalBooking)
                messageResult.onFailure {
                    android.util.Log.e("BookingRepo", "Messaggio chat non inviato")
                }
            }

            Result.success(finalBooking.idBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private suspend fun sendBookingSystemMessage(booking: Booking): Result<Unit> {
        return try {
            val guest = UsersRepository.getUserById(booking.idUtente).getOrThrow()!!
            val host = UsersRepository.getUserById(booking.idHost).getOrThrow()!!

            ChatRepository.initializeChat(
                currentUser = guest,
                otherUser = host,
                contextTitle = "Nuova prenotazione per ${booking.titoloCasa}",
                contextType = "BOOKING_SUMMARY",
                contextId = booking.idBooking,
                contextImageUrl = booking.url,
                customType = "booking_summary"
            ).getOrThrow()

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
                val hostRef = db.collection("users").document(booking.idHost)

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

                transaction.update(bookingRef, "stato", "Confermata")
                transaction.update(houseRef, "disponibilita", newAvailableList)
                transaction.update(hostRef, "numeroSoggiorni", com.google.firebase.firestore.FieldValue.increment(1))
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
     * Annulla una prenotazione:
     * Permesso se la prenotazione è ancora "In attesa" o "Pending".
     * Se è già "Confermata", si impedisce l'annullamento.
     */
    suspend fun deleteBooking(bookingId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val bookingRef = bookingCollection.document(bookingId)
                val bookingSnapshot = transaction.get(bookingRef)
                val booking = bookingSnapshot.toObject(Booking::class.java)
                    ?: throw Exception("Prenotazione non trovata")

                if (booking.stato != "In attesa" && booking.stato != "Pending") {
                    throw Exception("Non è possibile annullare una prenotazione già ${booking.stato}")
                }

                transaction.update(bookingRef, "stato", "Cancellata")
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BookingRepo", "Errore durante l'annullamento: ${e.message}")
            Result.failure(e)
        }
    }


}

