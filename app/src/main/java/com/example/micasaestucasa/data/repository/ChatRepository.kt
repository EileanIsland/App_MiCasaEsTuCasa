package com.example.micasaestucasa.data.repository

import androidx.core.net.toUri
import com.example.micasaestucasa.data.model.ChatPreview
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

object ChatRepository {
    private val firestore get() = FirebaseFirestore.getInstance()
    private val storage get() = FirebaseStorage.getInstance()
    private val chatsCollection = firestore.collection("chats")
    private var globalMessagesListener: ListenerRegistration? = null


    fun startGlobalChatListener(context: android.content.Context, currentUid: String) {    globalMessagesListener?.remove()

        globalMessagesListener = chatsCollection
            .whereArrayContains("users", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (docChange in snapshot.documentChanges) {
                    if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                        val doc = docChange.document

                        val lastSenderId = doc.getString("lastSenderId") ?: ""

                        if (lastSenderId == currentUid) continue

                        val lastTimestamp = doc.getLong("lastTimestamp") ?: 0L
                        val clearHistoryAtMap = doc.get("clearHistoryAt") as? Map<String, Long>
                        val clearTime = clearHistoryAtMap?.get(currentUid) ?: 0L

                        if (lastTimestamp > clearTime) {
                            val lastMessage = doc.getString("lastMessage") ?: ""
                            val userNames = doc.get("userNames") as? Map<String, String>
                            val users = doc.get("users") as? List<String> ?: emptyList()
                            val otherUserId = users.find { it != currentUid } ?: ""
                            val otherUserName = userNames?.get(otherUserId) ?: "Utente"

                            com.example.micasaestucasa.utils.NotificationHelper.sendChatNotification(
                                context,
                                "Messaggio da $otherUserName",
                                lastMessage,
                                doc.id
                            )
                        }
                    }
                }
            }
    }


    fun stopGlobalChatListener() {
        globalMessagesListener?.remove()
        globalMessagesListener = null
    }

    /**
     * Inizializza una chat
     */
    suspend fun initializeChat(
        currentUser: User,
        otherUser: User,
        contextId: String? = null,
        contextType: String? = null,
        contextTitle: String? = null,
        contextImageUrl: String? = null,
        customType: String = "context"
    ): Result<String> {
        return try {
            val chatId = getChatId(currentUser.id, otherUser.id)
            val chatRef = chatsCollection.document(chatId)

            val snapshot = chatRef.get().await()
            val now = System.currentTimeMillis()

            if (!snapshot.exists()) {
                val chatData = mutableMapOf(
                    "id" to chatId,
                    "users" to listOf(currentUser.id, otherUser.id),
                    "userNames" to mapOf(currentUser.id to currentUser.name, otherUser.id to otherUser.name),
                    "userPhotoUrls" to mapOf(currentUser.id to currentUser.profileImageUrl, otherUser.id to otherUser.profileImageUrl),
                    "lastTimestamp" to now,
                    "lastSenderId" to currentUser.id,
                    "hiddenBy" to emptyList<String>(),
                    "clearHistoryAt" to mapOf(currentUser.id to 0L, otherUser.id to 0L)
                )
                chatData["lastMessage"] = contextTitle ?: "Nuova conversazione"

                chatRef.set(chatData).await()
            }

            if (contextId != null && contextType != null) {
                val contextMessage = Message(
                    senderId = currentUser.id,
                    text = contextTitle ?: "",
                    type = customType,
                    contextId = contextId,
                    contextTitle = contextTitle,
                    contextType = contextType,
                    url = contextImageUrl,
                    timestamp = System.currentTimeMillis()
                )

                val sendResult = sendMessage(chatId, contextMessage)
                if (sendResult.isFailure) {
                    throw sendResult.exceptionOrNull() ?: Exception("Errore invio messaggio contesto")
                }
            }

            Result.success(chatId)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "Errore initializeChat: ${e.message}")
            Result.failure(e)
        }
    }




    /**
     * Invia un messaggio e "riattiva" la chat per tutti (svuota hiddenBy).
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
                "lastTimestamp" to finalMessage.timestamp,
                "lastSenderId" to finalMessage.senderId,
                "hiddenBy" to emptyList<String>()
            )

            batch.update(chatsCollection.document(chatId), chatUpdate)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Recupera le chat e FILTRA quelle che l'utente ha scelto di nascondere
     * o che sono state svuotate (messaggi vecchi).
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
                    val hiddenBy = doc.get("hiddenBy") as? List<String> ?: emptyList()
                    if (userId in hiddenBy) return@mapNotNull null

                    val clearHistoryAtMap = doc.get("clearHistoryAt") as? Map<String, Long>
                    val clearTime = clearHistoryAtMap?.get(userId) ?: 0L
                    val lastTimestamp = doc.getLong("lastTimestamp") ?: 0L

                    if (lastTimestamp <= clearTime) return@mapNotNull null

                    val users = doc.get("users") as? List<String> ?: return@mapNotNull null
                    val otherUserId = users.find { it != userId } ?: return@mapNotNull null
                    val namesMap = doc.get("userNames") as? Map<String, String>
                    val photosMap = doc.get("userPhotoUrls") as? Map<String, String>

                    ChatPreview(
                        id = doc.id,
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastTimestamp = lastTimestamp,
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
     * Nasconde la chat per l'utente corrente aggiungendo il suo ID a hiddenBy.
     */
    suspend fun deleteChat(chatId: String): Boolean {
        val currentUid = UsersRepository.getCurrentUid() ?: return false
        return try {
            val now = System.currentTimeMillis()

            chatsCollection.document(chatId)
                .update("hiddenBy", FieldValue.arrayUnion(currentUid),
                    "clearHistoryAt.$currentUid", now)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }


    suspend fun sendContextMessage(
        chatId: String, senderId: String, contextId: String,
        contextType: String, contextTitle: String, contextImageUrl: String?
    ): Result<Unit> {
        val contextMessage = Message(
            senderId = senderId, text = contextTitle, type = "context",
            contextId = contextId, contextType = contextType,
            url = contextImageUrl, timestamp = System.currentTimeMillis()
        )
        return sendMessage(chatId, contextMessage)
    }

    fun getChatId(uid1: String, uid2: String): String {
        if (uid1 == uid2) throw IllegalArgumentException("Utenti uguali")
        val sortedIds = listOf(uid1, uid2).sorted()
        return "${sortedIds[0]}_${sortedIds[1]}"
    }



    fun getMessages(chatId: String, currentUid: String): Flow<List<Message>> = callbackFlow {
        val chatRef = firestore.collection("chats").document(chatId)
        val messagesCollection = chatRef.collection("messages")
        var messagesListener: ListenerRegistration? = null

        val chatListener = chatRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val clearTime = if (snapshot != null && snapshot.exists()) {
                val clearHistoryAtMap = snapshot.get("clearHistoryAt") as? Map<String, Long>
                clearHistoryAtMap?.get(currentUid) ?: 0L
            } else 0L

            val startTime = clearTime - 10000L

            messagesListener?.remove()
            messagesListener = messagesCollection
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { querySnapshot, msgError ->
                    if (msgError != null) return@addSnapshotListener
                    val msgs = querySnapshot?.toObjects(Message::class.java) ?: emptyList()
                    trySend(msgs)
                }
        }

        awaitClose {
            chatListener.remove()
            messagesListener?.remove()
        }
    }


    suspend fun uploadImage(chatId: String, imageUri: String): Result<String> {
        return try {
            val fileName = "${java.util.UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("chats/$chatId/images/$fileName")
            ref.putFile(imageUri.toUri()).await()
            Result.success(ref.downloadUrl.await().toString())
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getChatMetadata(chatId: String, currentUserId: String): Flow<ChatPreview?> = callbackFlow {
        val subscription = chatsCollection.document(chatId).addSnapshotListener { snapshot, _ ->
            if (snapshot == null || !snapshot.exists()) { trySend(null); return@addSnapshotListener }
            val users = snapshot.get("users") as? List<String> ?: emptyList()
            val otherId = users.find { it != currentUserId } ?: ""
            val names = snapshot.get("userNames") as? Map<String, String>
            val photos = snapshot.get("userPhotoUrls") as? Map<String, String>
            trySend(ChatPreview(
                id = snapshot.id, lastMessage = snapshot.getString("lastMessage") ?: "",
                lastTimestamp = snapshot.getLong("lastTimestamp") ?: 0L,
                otherUserName = names?.get(otherId) ?: "Utente",
                otherUserPhoto = photos?.get(otherId), otherUserId = otherId
            ))
        }
        awaitClose { subscription.remove() }
    }

    suspend fun syncUserImageDataInChats(uid: String, newPhotoUrl: String): Result<Unit> {
        return try {
            val chats = chatsCollection.whereArrayContains("users", uid).get().await()
            val batch = firestore.batch()
            for (doc in chats.documents) { batch.update(doc.reference, "userPhotoUrls.$uid", newPhotoUrl) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}








