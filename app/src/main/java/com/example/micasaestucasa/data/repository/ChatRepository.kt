package com.example.micasaestucasa.data.repository

import androidx.core.net.toUri
import com.example.micasaestucasa.data.model.ChatPreview
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

object ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val chatsCollection = firestore.collection("chats")

    /**
     * Inizializza una chat - salvando anche i dati degli utenti.
     */
    /**
     * Inizializza una chat in modo ATOMICO.
     * Crea il documento chat e il messaggio di contesto insieme per evitare errori di permessi.
     */
    suspend fun initializeChat(
        currentUser: User,
        otherUser: User,
        contextId: String? = null,
        contextType: String? = null,
        contextTitle: String? = null,
        contextImageUrl: String? = null
    ): Result<String> {
        return try {
            val chatId = getChatId(currentUser.id, otherUser.id)
            val chatRef = chatsCollection.document(chatId)

            val snapshot = chatRef.get().await()

            if (!snapshot.exists()) {
                val batch = firestore.batch()

                val newChat = mapOf(
                    "id" to chatId,
                    "users" to listOf(currentUser.id, otherUser.id),
                    "userNames" to mapOf(
                        currentUser.id to (currentUser.name),
                        otherUser.id to (otherUser.name)
                    ),
                    "userPhotoUrls" to mapOf(
                        currentUser.id to (currentUser.profileImageUrl),
                        otherUser.id to (otherUser.profileImageUrl)
                    ),
                    "lastMessage" to (contextTitle ?: "Nuova conversazione"),
                    "lastTimestamp" to System.currentTimeMillis()
                )

                batch.set(chatRef, newChat)

                if (contextId != null && contextType != null) {
                    val messagesRef = chatRef.collection("messages").document()
                    val contextMessage = Message(
                        id = messagesRef.id,
                        senderId = currentUser.id,
                        text = contextTitle ?: "",
                        type = "context",
                        contextId = contextId,
                        contextType = contextType,
                        url = contextImageUrl,
                        timestamp = System.currentTimeMillis()
                    )
                    batch.set(messagesRef, contextMessage)
                }

                batch.commit().await()
                android.util.Log.d("CHAT_REPO", "Chat e contesto creati atomicamente")
            }
            Result.success(chatId)

        } catch (e: Exception) {
            android.util.Log.e("CHAT_REPO", "Errore inizializzazione atomica", e)
            Result.failure(e)
        }
    }

    /**
     * Invia un messaggio in una chat specifica.
     * Gestisce l'anteprima (lastMessage) in base al tipo di messaggio.
     */
    suspend fun sendMessage(chatId: String, message: Message): Result<Unit> {
        return try {
            val batch = firestore.batch()

            val messagesRef = chatsCollection.document(chatId).collection("messages")
            val newMsgDoc = messagesRef.document()
            val finalMessage = message.copy(id = newMsgDoc.id)

            batch.set(newMsgDoc, finalMessage)

            val previewText = when (finalMessage.type) {
                "image" -> "Foto"
                "context" -> "In merito a: ${finalMessage.text}"
                else -> finalMessage.text
            }

            val chatUpdate = mapOf(
                "lastMessage" to previewText,
                "lastTimestamp" to finalMessage.timestamp
            )

            batch.update(chatsCollection.document(chatId), chatUpdate)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    /**
     * Recupera la lista delle chat in TEMPO REALE.
     */
    fun getChatByUser(userId: String): Flow<List<ChatPreview>> = callbackFlow {
        val subscription = chatsCollection
            .whereArrayContains("users", userId)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val previews = snapshot?.documents?.mapNotNull { doc ->
                    val users = doc.get("users") as? List<String> ?: return@mapNotNull null
                    val otherUserId = users.find { it != userId } ?: return@mapNotNull null

                    val namesMap = doc.get("userNames") as? Map<String, String>
                    val photosMap = doc.get("userPhotoUrls") as? Map<String, String>

                    ChatPreview(
                        id = doc.id,
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastTimestamp = doc.getLong("lastTimestamp") ?: 0L,
                        otherUserName = namesMap?.get(otherUserId) ?: "Utente",
                        otherUserPhoto = photosMap?.get(otherUserId),
                        otherUserId = otherUserId
                    )
                } ?: emptyList()

                trySend(previews)
            }
        awaitClose { subscription.remove() }
    }


    /**
     * Invio contesto
     *
     */
    suspend fun sendContextMessage(
        chatId: String,
        senderId: String,
        contextId: String,
        contextType: String,
        contextTitle: String,
        contextImageUrl: String?
    ): Result<Unit>{
        return try {
            val messagesRef = chatsCollection.document(chatId).collection("messages").document()
            val contextMessage = Message(
                id = messagesRef.id,
                senderId = senderId,
                text = contextTitle,
                type = "context",
                contextId = contextId,
                contextType = contextType,
                url = contextImageUrl,
                timestamp = System.currentTimeMillis()
            )
            sendMessage(chatId, contextMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatId(uid1: String, uid2: String): String {
        if (uid1 == uid2) throw IllegalArgumentException("Utenti uguali")
        if (uid1.isBlank() || uid2.isBlank()) throw IllegalArgumentException("Utenti vuoti")
        val sortedIds = listOf(uid1, uid2).sorted()
        return "${sortedIds[0]}_${sortedIds[1]}"
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val subscription = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun uploadImage(chatId: String, imageUri: String): Result<String> {
        return try {
            val fileName = java.util.UUID.randomUUID().toString() + ".jpg"
            val ref = storage.reference.child("chats/$chatId/images/$fileName")
            ref.putFile(imageUri.toUri()).await()
            val url = ref.downloadUrl.await()
            Result.success(url.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatMetadata(chatId: String, currentUserId: String): Flow<ChatPreview?> = callbackFlow {
        val subscription = chatsCollection.document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val users = snapshot.get("users") as? List<String> ?: emptyList()
                val otherUserId = users.find { it != currentUserId } ?: ""
                val namesMap = snapshot.get("userNames") as? Map<String, String>
                val photosMap = snapshot.get("userPhotoUrls") as? Map<String, String>

                trySend(ChatPreview(
                    id = snapshot.id,
                    lastMessage = snapshot.getString("lastMessage") ?: "",
                    lastTimestamp = snapshot.getLong("lastTimestamp") ?: 0L,
                    otherUserName = namesMap?.get(otherUserId) ?: "Utente",
                    otherUserPhoto = photosMap?.get(otherUserId),
                    otherUserId = otherUserId
                ))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun deleteChat(chatId: String): Boolean {
        return try {
            val batch = firestore.batch()
            val chatRef = chatsCollection.document(chatId)
            val messages = chatRef.collection("messages").get().await()
            for (doc in messages) { batch.delete(doc.reference) }
            batch.delete(chatRef)
            batch.commit().await()
            true
        } catch (e: Exception) { false }
    }


    /**
     * Sincronizza i dati dell'utente (nome e foto) in tutte le sue chat aperte.
     * Firestore permette di aggiornare campi nidificati usando la "dot notation".
     */
    suspend fun syncUserImageDataInChats(uid: String, newPhotoUrl: String): Result<Unit> {
        return try {
            val chatsToUpdate = chatsCollection
                .whereArrayContains("users", uid)
                .get()
                .await()

            if (chatsToUpdate.isEmpty) return Result.success(Unit)

            val batch = firestore.batch()

            for (doc in chatsToUpdate.documents) {
                val chatRef = doc.reference
                batch.update(chatRef, "userPhotoUrls.$uid", newPhotoUrl)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}









