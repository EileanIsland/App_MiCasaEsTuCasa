package com.example.micasaestucasa.ui.models

import com.example.micasaestucasa.data.model.Report
import com.example.micasaestucasa.data.model.User

/**
 *
 */
data class ReportedUser(
    val user: User,
    val reports: List<Report> = emptyList(),
    var isExpanded: Boolean = false
)