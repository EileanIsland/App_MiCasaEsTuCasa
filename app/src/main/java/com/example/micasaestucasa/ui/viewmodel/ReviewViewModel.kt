package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.Review
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
    val isPublishEnabled: Boolean = false
)

class ReviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState = _uiState.asStateFlow()


    fun initialize(targetId: String, targetType: String) {
        _uiState.update { it.copy(targetId = targetId, targetType = targetType) }
    }


    fun updateRating(rating: Float) {
        _uiState.update { it.copy(rating = rating, isPublishEnabled = rating > 0f) }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(reviewText = text) }
    }


    fun submitReview() {
        val state = _uiState.value
        if (!state.isPublishEnabled) return

        val currentUid = UsersRepository.getCurrentUid() ?: throw Exception("Effettuare il login per pubblicare una recensione")
        val currentUserName = UsersRepository.currentUserProfile.value?.name ?: throw Exception("Effettuare il login per pubblicare una recensione")
        val currentUserImageUrl = UsersRepository.currentUserProfile.value?.profileImageUrl ?: throw Exception("Effettuare il login per pubblicare una recensione")

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val review = Review(
                id = "",
                targetId = state.targetId,
                rating = state.rating,
                testo = state.reviewText,
                reviewerId = currentUid,
                reviewerName = currentUserName,
                reviewerImageUrl = currentUserImageUrl,
                targetTypeString = state.targetType
            )

            ReviewRepository.saveReviewHouse(review).onSuccess {
                _uiState.update { it.copy(
                    isLoading = false,
                    isSuccess = true
                ) }
            }.onFailure { e ->
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Errore durante il salvataggio"
                ) }
            }
        }

    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}