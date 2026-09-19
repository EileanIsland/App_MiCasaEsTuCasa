package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.Tag
import com.example.micasaestucasa.data.model.TagCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

object TagsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tagsCollection = db.collection("tags")

    /**
     * Recupera tutte le tag di un determinato tipo
     */
    suspend fun getTagsByType(tipo: String): List<String> {
        return try {
            val querySnapshot = tagsCollection
                .whereEqualTo("tipo", tipo.trim().lowercase())
                .get()
                .await()
            querySnapshot.documents.mapNotNull { it.getString("nome") }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Salva un nuovo tag
     */
    suspend fun saveTag(tag: String, tipo:String) {
        if(tipo.isBlank() or tag.isBlank())
            return

        try {
            val newDocRef = tagsCollection.document()

            val categoria = when (tipo.lowercase()) {
                "servizi" -> TagCategory.SERVIZIO
                "regole" -> TagCategory.REGOLA
                "esperienza" -> TagCategory.ESPERIENZA
                else -> throw IllegalArgumentException("Tipo di tag non valido")
            }

            val nuovoTag = Tag(tag, categoria)

            newDocRef.set(nuovoTag).await()

        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Elimina un tag esistente
     */
    suspend fun deleteTag(tag: Tag) {
        try {
            tagsCollection.document(tag.nome).delete().await()
        } catch (e: Exception) {
            throw e
        }
    }
}





