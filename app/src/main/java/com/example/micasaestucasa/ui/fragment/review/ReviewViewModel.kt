package com.example.micasaestucasa.ui.fragment.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.ReviewRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val targetId: String = "",
    val targetType: String = "",
    val rating: Float = 0f,
    val reviewText: String = "",
    val isPublishEnabled: Boolean = false,
    val bookingId: String = ""
)

class ReviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState = _uiState.asStateFlow()

    fun initialize(targetId: String, targetType: String, bookingId: String) {
        _uiState.update {
            it.copy(
                targetId = targetId,
                targetType = targetType,
                bookingId = bookingId,
                isSuccess = false,
                errorMessage = null
            )
        }
    }

    fun updateRating(rating: Float) {
        _uiState.update { it.copy(rating = rating, isPublishEnabled = rating > 0f) }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(reviewText = text) }
    }

    /**
     * Invia la recensione (a una Casa o a un Utente).
     * Gestisce i controlli di sicurezza e i permessi Firestore.
     */
    fun submitReview() {
        val state = _uiState.value
        if (state.targetId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Errore: ID destinatario mancante.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val currentUid = UsersRepository.getCurrentUid()
            val profile = UsersRepository.currentUserProfile.value

            if (currentUid == null || profile == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Effettuare il login per pubblicare una recensione"
                    )
                }
                return@launch
            }

            val review = Review(
                id = "",
                reviewerId = currentUid,
                targetId = state.targetId,
                targetTypeString = state.targetType,
                bookingId = state.bookingId,
                rating = state.rating,
                testo = state.reviewText,
                reviewerName = profile.name,
                reviewerImageUrl = profile.profileImageUrl,

            )

            val result = if (state.targetType == "CASA") ReviewRepository.saveReviewHouse(review)
            else ReviewRepository.saveReviewUser(review)

            result.onSuccess {
                if (state.bookingId.isNotEmpty()) {
                    BookingRepository.markAsReviewed(state.bookingId)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isSuccess = true
                                )
                            }
                        }
                        .onFailure { e ->
                            handleError(e, "Recensione inviata, ma errore nell'aggiornamento prenotazione")
                        }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    }
                }
            }.onFailure { e ->
                handleError(e, "Errore durante il salvataggio della recensione")
            }
        }
    }

    /**
     * Gestione centralizzata degli errori per intercettare PERMISSION_DENIED
     */
    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi per pubblicare questa recensione."
        } else {
            "$context: ${e.localizedMessage}"
        }
        _uiState.update {
            it.copy(isLoading = false, errorMessage = message)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}





