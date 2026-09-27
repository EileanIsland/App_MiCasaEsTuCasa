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
    suspend fun updateBookingStatus(bookingId: String, newStatus: String): Result<Unit> {
        return try {
            bookingCollection.document(bookingId).update("stato", newStatus).await()
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

