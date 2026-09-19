package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.model.User

data class UserProfileUiState (
    val isLoading: Boolean = true,
    val user: User? = null,
    val userStats: UserStats? = null,
    val reviews: List<Review> = emptyList(),
    val isShowingAllReviews: Boolean = false,
    val errorMessage: String? = null

)