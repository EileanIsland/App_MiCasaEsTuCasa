package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.model.User
import com.example.micasaestucasa.ui.models.UserStats

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val user: User? = null,
    val userReviewsReceived: List<Review> = emptyList(),
    val userStats: UserStats? = null,
    val isShowingAllReviews: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)
