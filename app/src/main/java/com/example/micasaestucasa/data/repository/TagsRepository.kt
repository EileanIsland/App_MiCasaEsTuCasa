package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Tag
import com.example.micasaestucasa.data.model.TagCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object TagsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tagsCollection = db.collection("tags")

    /**
     * Recupera tutte le tag di un determinato tipo
     */
    suspend fun getTagsByType(tipo: String): Result<List<String>> {
        return try {
            val querySnapshot = tagsCollection
                .whereEqualTo("tipo", tipo.trim().lowercase())
                .get()
                .await()
            val tags = querySnapshot.documents.mapNotNull { it.getString("nome") }
            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Salva un nuovo tag
     */
    suspend fun saveTag(tag: String, tipo:String) : Result<Unit> {
        if(tipo.isBlank() or tag.isBlank())
            return Result.failure(IllegalArgumentException("Tag o tipo non possono essere vuoti"))

        return try {
            val newDocRef = tagsCollection.document()

            val categoria = when (tipo.lowercase()) {
                "servizi" -> TagCategory.SERVIZIO
                "regole" -> TagCategory.REGOLA
                "esperienza" -> TagCategory.ESPERIENZA
                else -> throw IllegalArgumentException("Tipo di tag non valido")
            }

            val nuovoTag = Tag(tag, categoria)

            newDocRef.set(nuovoTag).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

    /**
     * Elimina un tag esistente
     */
    suspend fun deleteTag(tag: Tag): Result<Unit> {
        return try {
            tagsCollection.document(tag.nome).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}





