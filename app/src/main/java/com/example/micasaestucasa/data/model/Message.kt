package com.example.micasaestucasa.data.model

data class Message(
    val id: String = "",
    val text: String = "",
    val url: String? = null,
    val type: String = "text", // text or image or context
    val senderId: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    val contextId: String? = null,
    val contextType: String? = null, //HOUSE o BOOKING o BOOKING_SUMMARY
    val contextTitle: String? = null
)
