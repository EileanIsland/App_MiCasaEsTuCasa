package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.model.ReviewTarget
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object ReviewRepository {

    private val db = FirebaseFirestore.getInstance()
    private val reviewsCollection = db.collection("reviews")
    private val bookingsCollection = db.collection("bookings")

    /**
     * Salva una recensione per la CASA e aggiorna il flag 'houseReviewed' nel Booking.
     */
    suspend fun saveReviewHouse(review: Review): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val houseRef = db.collection("case").document(review.targetId)
                val bookingRef = bookingsCollection.document(review.bookingId)
                val newReviewRef = reviewsCollection.document()

                val houseSnapshot = transaction.get(houseRef)
                val oldRating = houseSnapshot.getDouble("valutazioneMedia") ?: 0.0
                val oldCount = houseSnapshot.getLong("numeroRecensioni") ?: 0L

                val newCount = oldCount + 1
                val newRating = ((oldRating * oldCount) + review.rating) / newCount

                transaction.set(newReviewRef, review.copy(id = newReviewRef.id))

                transaction.update(houseRef, mapOf(
                    "valutazioneMedia" to newRating,
                    "numeroRecensioni" to newCount
                ))

                transaction.update(bookingRef, "isHouseReviewed", true)

            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Salva una recensione per l'UTENTE e aggiorna il flag 'userReviewed' nel Booking.
     */
    suspend fun saveReviewUser(review: Review): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val userRef = db.collection("users").document(review.targetId)
                val bookingRef = bookingsCollection.document(review.bookingId)
                val newReviewRef = reviewsCollection.document()

                val userSnapshot = transaction.get(userRef)
                val currentRating = userSnapshot.getDouble("valutazioneMedia") ?: 0.0
                val currentNumReviews = userSnapshot.getLong("numeroRecensioni") ?: 0L

                val newNumReviews = currentNumReviews + 1
                val newRating = ((currentRating * currentNumReviews) + review.rating) / newNumReviews

                transaction.set(newReviewRef, review.copy(id = newReviewRef.id))

                transaction.update(userRef,
                    "numeroRecensioni", newNumReviews,
                    "valutazioneMedia", newRating
                )

                transaction.update(bookingRef, "isUserReviewed", true)

            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }


    suspend fun getReviewsForHouse(houseId: String): Result<List<Review>> = try {
        val snapshot = reviewsCollection
            .whereEqualTo("targetId", houseId)
            .whereEqualTo("targetType", ReviewTarget.CASA.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()
        Result.success(snapshot.toObjects(Review::class.java))
    } catch (e: Exception) { Result.failure(e) }




    suspend fun getReviewsForUser(userId: String): Result<List<Review>> = try {
        val snapshot = reviewsCollection
            .whereEqualTo("targetId", userId)
            .whereEqualTo("targetType", ReviewTarget.UTENTE.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()
        Result.success(snapshot.toObjects(Review::class.java))
    } catch (e: Exception) { Result.failure(e) }


}


