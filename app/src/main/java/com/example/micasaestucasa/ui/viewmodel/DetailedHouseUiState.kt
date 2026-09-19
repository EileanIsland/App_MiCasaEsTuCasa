package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.data.model.User

data class DetailedHouseUiState(
    val isLoading: Boolean = true,
    val house: Casa? = null,
    val owner: User? = null,
    val ownerStat: UserStats = UserStats(),
    val ownerReviews: List<Review> = emptyList(),
    val houseReviews: List<Review> = emptyList(),
    val houseRating: Double = 0.0,
    val numRec: Int = 0,
    val isShowingAllReviews: Boolean = false,
    val errorMessage: String? = null,
    val isHouseNotFound: Boolean = false
)
