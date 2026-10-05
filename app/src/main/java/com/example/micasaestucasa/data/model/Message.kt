package com.example.micasaestucasa.data.model

data class Message(
    val id: String = "",
    val text: String = "",
    val url: String? = null,
    val type: String = "text",
    val senderId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
