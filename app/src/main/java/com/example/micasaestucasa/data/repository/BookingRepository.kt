package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookingCollection = db.collection("bookings")

    /**
     * Recupera tutte le prenotazioni effettuate da un utente specifico
     */
    suspend fun getBookingByGuest(guestId: String): List<Booking> {
        return try {
            val snapshot = bookingCollection
                .whereEqualTo("idUtente", guestId)
                .whereEqualTo("stato", "accepted")
                .get()
                .await()
            snapshot.toObjects(Booking::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Recupera tutte le prenotazioni che sono richieste a un proprietario specifico
     */
    suspend fun getBookingByHost(hostId: String): List<Booking> {
        return try {
            val snapshot = bookingCollection
                .whereEqualTo("idHost", hostId)
                .get()
                .await()
            snapshot.toObjects(Booking::class.java)
        }
        catch (e: Exception) {
            emptyList()
        }
    }


    /**
     * Salva o aggiorna i dati di una prenotazione
     */
    suspend fun saveBooking(booking: Booking): String {
        return try {
            val docRef = if (booking.idBooking.isEmpty()) {
                bookingCollection.document()
            } else {
                bookingCollection.document(booking.idBooking)
            }
            val finalBooking = booking.copy(idBooking = docRef.id)
            docRef.set(finalBooking).await()
            finalBooking.idBooking
        } catch (e: Exception) {
            throw e
        }
    }


    /**
     * recupera le info di una prenotazione tramite id
     */
    suspend fun getBookingById(booingId: String): Booking{
        return try{
            val snapshot = bookingCollection.document(booingId).get().await()
            snapshot.toObject(Booking::class.java) ?: throw Exception("Booking not found")
        }catch (e: Exception){
            throw e
        }
    }


    /**
     * elimina una prenotazione
     */
    suspend fun deleteBooking(booking: Booking){
        try{
            bookingCollection.document(booking.idBooking).delete().await()
        }catch (e: Exception){
            throw e
        }
    }

    /**
     * recupera il numero di prenotazioni effettuate da un utente specifico
     */
    suspend fun getBookingCountByGuest(guestId: String): Int {
        return try {
            val snapshot = bookingCollection
                .whereEqualTo("idUtente", guestId)
                .whereEqualTo("stato", "accepted")
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }



}