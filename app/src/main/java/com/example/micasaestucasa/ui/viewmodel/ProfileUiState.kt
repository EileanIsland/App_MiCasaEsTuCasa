package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.model.User

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val userReviewsReceived: List<Review> = emptyList(),
    val userStats: UserStats? = null,
    val isShowingAllReviews: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)
