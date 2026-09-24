package com.example.micasaestucasa.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.UUID

object StorageRepository {
    private val storage = Firebase.storage.reference

    /**
     * Carica una lista di immagini su Firebase Storage in PARALLELO.
     * Restituisce un Result contenente solo gli URL caricati con successo.
     * Se l'operazione viene annullata (es. cambio schermata), i caricamenti residui si bloccano subito.
     */
    suspend fun upLoadImages(uris: List<String>): Result<List<String>> = coroutineScope {
        try {
            // Avvia tutti i caricamenti contemporaneamente in background
            val deferredUploads = uris.map { uriString ->
                async {
                    try {
                        val uri = uriString.toUri()
                        val imageRef = storage.child("houses/${UUID.randomUUID()}.jpeg")

                        imageRef.putFile(uri).await()
                        imageRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        // Se una singola foto fallisce, restituisce null invece di far fallire tutto
                        null
                    }
                }
            }

            val urls = deferredUploads.awaitAll().filterNotNull()

            Result.success(urls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Carica la foto profilo di un utente specifico. Utilizza l'UID come nome file
     * per sovrascrivere automaticamente la vecchia foto ed evitare duplicati.
     */
    suspend fun uploadProfileImage(uid: String, uriString: String): Result<String> {
        return try {
            val uri = uriString.toUri()
            val imageRef = storage.child("users/$uid/profile.jpeg")

            imageRef.putFile(uri).await()
            val url = imageRef.downloadUrl.await().toString()

            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina una singola immagine dallo Storage a partire dal suo URL pubblico.
     * Utile se l'utente rimuove una foto durante la modifica di un annuncio.
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val fileRef = Firebase.storage.getReferenceFromUrl(imageUrl)
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un'intera lista di immagini dallo Storage in PARALLELO.
     * Molto più veloce rispetto al vecchio ciclo sequenziale quando si elimina una casa intera.
     */
    suspend fun deleteImages(imageUrls: List<String>): Result<Unit> = coroutineScope {
        try {
            val deferredDeletions = imageUrls.map { url ->
                async {
                    try {
                        Firebase.storage.getReferenceFromUrl(url).delete().await()
                    } catch (e: Exception) {
                        // Ignora il singolo errore di eliminazione per non bloccare la pulizia delle altre foto
                    }
                }
            }

            // Attende la fine di tutte le eliminazioni
            deferredDeletions.awaitAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}



