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
     * Inizializza una chat - salvando anche i dati degli utenti
     */
    suspend fun initializeChat(currentUser: User, otherUser: User ): Result<String>{
        return try{
            val chatId = getChatId(currentUser.id, otherUser.id)
            val chatRef = chatsCollection.document(chatId)
            val snapsht =chatRef.get().await()

            if(!snapsht.exists()){
                val newChat = mapOf(
                    "id" to chatId,
                    "users" to listOf(currentUser.id, otherUser.id),

                    "userNames" to mapOf(
                        currentUser.id to currentUser.name,
                        otherUser.id to otherUser.name
                    ),
                    "userPhotoUrls" to mapOf(
                        currentUser.id to currentUser.profileImageUrl,
                        otherUser.id to otherUser.profileImageUrl
                    ),
                    "lastMessage" to "",
                    "lastTimestamp" to System.currentTimeMillis()
                )
                chatRef.set(newChat).await()
            }
            Result.success(chatId)

        }catch(e: Exception){
            Result.failure(e)
        }

    }


    /**
     * Recupera la lista delle chat in TEMPO REALE (molto più efficiente)
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

                    // Recuperiamo i dati direttamente dal documento chat (zero query extra!)
                    val namesMap = doc.get("userNames") as? Map<String, String>
                    val photosMap = doc.get("userPhotos") as? Map<String, String>

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
     * Invia un messaggio in un chat specifico.
     */
    suspend fun sendMessage(chatId: String, message: Message): Result<Unit>{
        return try{
            val batch = firestore.batch()

            val messagesRef = chatsCollection.document(chatId).collection("messages")
            val newMsgDoc = messagesRef.document()
            val finalMessage = message.copy(id = newMsgDoc.id)

            batch.set(newMsgDoc, finalMessage)

            val chatUpdate = mapOf(
                "lastMessage" to if (finalMessage.type == "image") "Foto" else finalMessage.text,
                "lastTimestamp" to finalMessage.timestamp
            )

            batch.update(chatsCollection.document(chatId), chatUpdate)

            batch.commit().await()
            Result.success(Unit)
        }catch(e: Exception){
            Result.failure(e)
        }

    }


    fun getChatId(uid1: String, uid2: String): String {
        if(uid1 == uid2) throw IllegalArgumentException("Utenti uguali")
        if(uid1.isBlank() || uid2.isBlank()) throw IllegalArgumentException("Utenti vuoti")

        val sortedIdas = listOf(uid1, uid2).sorted()
        return "${sortedIdas[0]}_${sortedIdas[1]}"
    }


    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val subscription = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if(error != null){
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose{subscription.remove()}
    }


    suspend fun uploadImage(chatId: String, imageUri: String): Result<String>{
        return try{
            val fileName = java.util.UUID.randomUUID().toString() + ".jpg"
            val ref = storage.reference.child("chats/$chatId/images/$fileName")
            ref.putFile(imageUri.toUri()).await()
            val url = ref.downloadUrl.await()

            Result.success(url.toString())
        }catch(e: Exception){
            Result.failure(e)
        }
    }



    /**
     * Recupera i metadati di una singola chat in tempo reale.
     * Restituisce un Flow del modello ChatPreview.
     */
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

                // Estraiamo i dati dal documento
                val users = snapshot.get("users") as? List<String> ?: emptyList()
                val otherUserId = users.find { it != currentUserId } ?: ""

                val namesMap = snapshot.get("userNames") as? Map<String, String>
                // Nota: uso "userPhotoUrls" per coerenza con initializeChat
                val photosMap = snapshot.get("userPhotoUrls") as? Map<String, String>

                val preview = ChatPreview(
                    id = snapshot.id,
                    lastMessage = snapshot.getString("lastMessage") ?: "",
                    lastTimestamp = snapshot.getLong("lastTimestamp") ?: 0L,
                    otherUserName = namesMap?.get(otherUserId) ?: "Utente",
                    otherUserPhoto = photosMap?.get(otherUserId),
                    otherUserId = otherUserId
                )

                trySend(preview)
            }
        awaitClose { subscription.remove() }
    }


    /**
     * Elimina la chat e tutti i relativi messaggi (Batch)
     */
    suspend fun deleteChat(chatId: String): Boolean {
        return try {
            val batch = firestore.batch()

            val chatRef = chatsCollection.document(chatId)

            val messages = chatRef.collection("messages").get().await()
            for (doc in messages) {
                batch.delete(doc.reference)
            }

            batch.delete(chatRef)

            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }


}


//TODO: aggiornare regole del db
// match /chats/{chatId} {
//  allow read, write: if request.auth.uid in resource.data.users;
//      match /messages/{messageId} {
//    allow read, write: if request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.users;
//  }
//}







