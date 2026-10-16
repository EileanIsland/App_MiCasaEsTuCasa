package com.example.micasaestucasa.data.model

data class ReportedUser(
    val user: User,
    val reports: List<Report> = emptyList(),
    var isExpanded: Boolean = false
)
