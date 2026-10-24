package com.example.micasaestucasa.ui.fragment.profile

import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.model.User
import com.example.micasaestucasa.data.model.UserStats

data class UserProfileUiState (
    val isLoading: Boolean = true,
    val user: User? = null,
    val userStats: UserStats? = null,
    val reviews: List<Review> = emptyList(),
    val isShowingAllReviews: Boolean = false,
    val errorMessage: String? = null,
    val feedback: String? = null

)