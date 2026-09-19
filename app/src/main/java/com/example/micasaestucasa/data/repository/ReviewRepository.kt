package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.model.ReviewTarget
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ReviewRepository {

    private val db = FirebaseFirestore.getInstance()
    private val reviewsCollection = db.collection("reviews")

    //Salva una nuova recensione
    suspend fun saveReview(review: Review): String {
        return try {
            val docRef = if (review.id.isEmpty()) {
                reviewsCollection.document()
            } else {
                reviewsCollection.document(review.id)
            }

            val finalReview = review.copy(id = docRef.id)
            docRef.set(finalReview).await()
            finalReview.id
        } catch (e: Exception) {
            throw e
        }
    }

    // 2. Recupera recensioni per una CASA
    suspend fun getReviewsForHouse(houseId: String): List<Review> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("targetId", houseId)
                .whereEqualTo("targetType", ReviewTarget.CASA.name)
                .get()
                .await()
            snapshot.toObjects(Review::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Recupera recensioni per un UTENTE
    suspend fun getReviewsForUser(userId: String): List<Review> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("targetId", userId)
                .whereEqualTo("targetType", ReviewTarget.UTENTE.name)
                .get()
                .await()
            snapshot.toObjects(Review::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }


    /**
     * Calcola le statistiche di rating (Media e Numero)
     * In Firestore è meglio calcolarle al volo dai documenti scaricati
     */
    suspend fun getRatingStats(targetId: String, type: ReviewTarget): Pair<Double, Int> {
        val reviews = if (type == ReviewTarget.CASA) {
            getReviewsForHouse(targetId)
        } else {
            getReviewsForUser(targetId)
        }

        if (reviews.isEmpty()) return Pair(0.0, 0)

        val average = reviews.map { it.initialRating }.average()
        return Pair(average, reviews.size)
    }
}



    /**

    private val reviews = mutableListOf(

        // =========================
        // CASA 001
        // =========================

        Review(
            id = "review001",
            reviewerId = "user004",
            targetId = "casa001",
            targetType = ReviewTarget.CASA,
            initialRating = 5,
            testo = "Casa pulita, vista spettacolare e proprietario molto disponibile.",
            date = "2026-06-18".toLongDate(),

            reviewerName = "Sara Verdi",
            reviewerImageUrl = "https://example.com/sara.jpg"
        ),

        Review(
            id = "review002",
            reviewerId = "user002",
            targetId = "casa001",
            targetType = ReviewTarget.CASA,
            initialRating = 4,
            testo = "Ottima posizione, appartamento confortevole.",
            date = "2026-07-03".toLongDate(),

            reviewerName = "Elena Bianchi",
            reviewerImageUrl = "https://example.com/elena.jpg"
        ),

        // =========================
        // CASA 002
        // =========================

        Review(
            id = "review003",
            reviewerId = "user001",
            targetId = "casa002",
            targetType = ReviewTarget.CASA,
            initialRating = 5,
            testo = "Loft moderno e in pieno centro, perfetto per un weekend.",
            date = "2026-05-22".toLongDate(),

            reviewerName = "Marco Rossi",
            reviewerImageUrl = "https://example.com/marco.jpg"
        ),

        // =========================
        // CASA 003
        // =========================

        Review(
            id = "review004",
            reviewerId = "user004",
            targetId = "casa003",
            targetType = ReviewTarget.CASA,
            initialRating = 5,
            testo = "Villa stupenda, piscina bellissima e tanta tranquillità.",
            date = "2026-07-20".toLongDate(),

            reviewerName = "Sara Verdi",
            reviewerImageUrl = "https://example.com/sara.jpg"
        ),

        Review(
            id = "review005",
            reviewerId = "user001",
            targetId = "casa003",
            targetType = ReviewTarget.CASA,
            initialRating = 4,
            testo = "Esperienza davvero piacevole, consigliata.",
            date = "2026-07-26".toLongDate(),

            reviewerName = "Marco Rossi",
            reviewerImageUrl = "https://example.com/marco.jpg"
        ),

        // =========================
        // RECENSIONI UTENTI
        // =========================

        Review(
            id = "review006",
            reviewerId = "user004",
            targetId = "user001",
            targetType = ReviewTarget.UTENTE,
            initialRating = 5,
            testo = "Host gentilissimo e sempre disponibile.",
            date = "2026-06-19".toLongDate(),

            reviewerName = "Sara Verdi",
            reviewerImageUrl = "https://example.com/sara.jpg"
        ),

        Review(
            id = "review007",
            reviewerId = "user001",
            targetId = "user002",
            targetType = ReviewTarget.UTENTE,
            initialRating = 5,
            testo = "Comunicazione rapida e check-in impeccabile.",
            date = "2026-05-23".toLongDate(),

            reviewerName = "Marco Rossi",
            reviewerImageUrl = "https://example.com/marco.jpg"
        ),

        Review(
            id = "review008",
            reviewerId = "user002",
            targetId = "user003",
            targetType = ReviewTarget.UTENTE,
            initialRating = 4,
            testo = "Host preciso e molto cordiale.",
            date = "2026-07-28".toLongDate(),

            reviewerName = "Elena Bianchi",
            reviewerImageUrl = "https://example.com/elena.jpg"
        )

    )

    fun getReviews(): List<Review> = reviews

    fun getReviewById(id: String): Review? =
        reviews.find { it.id == id }

    fun getReviewsForHouse(houseId: String): List<Review> =
        reviews.filter {
            it.targetType == ReviewTarget.CASA &&
                    it.targetId == houseId
        }

    fun getReviewsForUser(userId: String): List<Review> =
        reviews.filter {
            it.targetType == ReviewTarget.UTENTE &&
                    it.targetId == userId
        }

    fun getReviewsByIds(reviewIds: List<String>): List<Review> =
        reviews.filter { reviewIds.contains(it.id) }


    fun getRatingStats(reviewsIds: List<String>): Pair<Double, Int>{
        val reviews = getReviewsByIds(reviewsIds)

        if(reviews.isEmpty()) return Pair(0.0, 0)

        val avarage = reviews.map { it.initialRating }.average()

        return Pair(avarage, reviews.size)

    }
    */
