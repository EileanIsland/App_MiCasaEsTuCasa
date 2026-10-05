package com.example.micasaestucasa.data.model

data class ChatPreview (
    val id: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,
    val otherUserPhoto: String? = null,
    val otherUserName: String = "",
    val otherUserId: String = ""

)