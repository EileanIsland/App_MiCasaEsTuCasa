package com.example.micasaestucasa.data.repository

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AdminRepository {
    private val db get() = FirebaseFirestore.getInstance()
    private val bookingRef get() = db.collection("bookings")

    suspend fun getBookingStats(): Result<Map<String, Int>> {
        return try {
            val now = System.currentTimeMillis()

            val totalTask = bookingRef.count().get(AggregateSource.SERVER).await()

            val concludedTask = bookingRef
                .whereEqualTo("stato", "Confermata")
                .whereLessThan("dataFine", now)
                .count().get(AggregateSource.SERVER).await()

            val ongoingTask = bookingRef
                .whereEqualTo("stato", "Confermata")
                .whereLessThanOrEqualTo("dataInizio", now)
                .whereGreaterThanOrEqualTo("dataFine", now)
                .count().get(AggregateSource.SERVER).await()

            val upcomingTask = bookingRef
                .whereEqualTo("stato", "Confermata")
                .whereGreaterThan("dataInizio", now)
                .count().get(AggregateSource.SERVER).await()

            val pendingTask = bookingRef
                .whereIn("stato", listOf("In attesa", "Pending"))
                .count().get(AggregateSource.SERVER).await()

            Result.success(mapOf(
                "total" to totalTask.count.toInt(),
                "concluded" to concludedTask.count.toInt(),
                "ongoing" to ongoingTask.count.toInt(),
                "upcoming" to upcomingTask.count.toInt(),
                "pending" to pendingTask.count.toInt()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




}