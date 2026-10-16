package com.example.micasaestucasa.data.model


data class Report(
    val id: String = "",
    val reportedUserId: String = "",
    val reporterUserId: String = "",
    val status: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val timestamp: Long = 0
)
