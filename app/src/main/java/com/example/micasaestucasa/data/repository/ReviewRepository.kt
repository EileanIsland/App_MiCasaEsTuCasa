package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.model.ReviewTarget
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object ReviewRepository {

    private val db = FirebaseFirestore.getInstance()
    private val reviewsCollection = db.collection("reviews")

    /**
     * Salva una nuova recensione o ne aggiorna una esistente.
     * Restituisce Result.success(Unit) se l'operazione va a buon fine.
     */
    suspend fun saveReview(review: Review): Result<Unit> {
        return try {
            val docRef = if (review.id.isBlank()) {
                reviewsCollection.document()
            } else {
                reviewsCollection.document(review.id)
            }

            val finalReview = review.copy(id = docRef.id)
            docRef.set(finalReview).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera le recensioni per una CASA.
     */
    suspend fun getReviewsForHouse(houseId: String): Result<List<Review>> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("targetId", houseId)
                .whereEqualTo("targetType", ReviewTarget.CASA.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val reviews = snapshot.toObjects(Review::class.java)
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera le recensioni per un UTENTE.
     */
    suspend fun getReviewsForUser(userId: String): Result<List<Review>> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("targetId", userId)
                .whereEqualTo("targetType", ReviewTarget.UTENTE.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val reviews = snapshot.toObjects(Review::class.java)
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcola le statistiche di rating (Media e Numero totale).
     */
    suspend fun getRatingStats(targetId: String, type: ReviewTarget): Result<Pair<Double, Int>> {
        return try {
            val result = if (type == ReviewTarget.CASA) {
                getReviewsForHouse(targetId)
            } else {
                getReviewsForUser(targetId)
            }

            val reviews = result.getOrThrow()

            if (reviews.isEmpty()) {
                return Result.success(Pair(0.0, 0))
            }

            val average = reviews.map { it.rating.toDouble() }.average()
            Result.success(Pair(average, reviews.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica se un utente ha già lasciato una recensione per una specifica prenotazione.
     */
    suspend fun hasAlreadyReviewed(bookingId: String, authorId: String): Result<Boolean> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("bookingId", bookingId)
                .whereEqualTo("authorId", authorId)
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
