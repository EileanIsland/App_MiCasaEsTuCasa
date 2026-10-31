package com.example.micasaestucasa.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

object AuthRepository {

    private val auth get() = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentIUD(): String? = auth.currentUser?.uid


    /**
     * Esegue il login.
     * Restituisce Result per permettere al ViewModel di gestire i vari tipi di errore
     * (es. Password errata, utente non trovato, rete assente).
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Utente non trovato dopo il login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Crea un nuovo account.
     */
    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Errore durante la creazione dell'utente"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun signOut() {
        auth.signOut()
    }
}



