package com.example.micasaestucasa.data.repository

import com.example.micasaestucasa.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await


/**
 * Repository per la gestione degli utenti su Firebase Firestore.
 * Utilizza il pattern Singleton (object) e integra una cache reattiva tramite StateFlow.
 */
object UsersRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    // Cache del profilo utente loggato
    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()


    /**
     * Carica il profilo e aggiorna la cache locale.
     *
     * @param uid l'ID dell'utente da cercare.
     * @return [Result] di tipo [Unit] che indica se l'operazione è andata a buon fine.
     */
    suspend fun loadAndCacheProfile(uid: String): Result<Unit> {
        return getUserById(uid).fold(
            onSuccess = { user ->
                if (user != null) {
                    _currentUserProfile.value = user
                }
                Result.success(Unit)
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }

    /**
     * Verifica se l'utente loggato ha i permessi amministrativi
     * @return true se l'utente è amministratore, false altrimenti
     */
    fun isAdmin(): Boolean = _currentUserProfile.value?.ruolo == "admin"

    /**
     * Restiruis l'UID dell'utente autenticato su Firebase Auth.
     * @return l'UID dell'utente o null se non è autenticato.
     */
    fun getCurrentUid(): String? = auth.currentUser?.uid

    /**
     * Recupera il profilo dell'utente loggato cercando prima in cache.
     * Riduce le letture a Firestore (risparmio costi).
     * @return [Result] di tipo [User] che contiene il profilo dell'utente.
     */
    suspend fun getCurrentUserProfile(): Result<User?> {
        val uid = getCurrentUid() ?: return Result.success(null)

        if (_currentUserProfile.value?.id == uid) {
            return Result.success(_currentUserProfile.value)
        }

        return getUserById(uid).onSuccess { user ->
            _currentUserProfile.value = user
        }
    }

    /**
     * Recupera un utente specifico tramite ID.
     * @param id l'ID dell'utente da cercare.
     */
    suspend fun getUserById(id: String): Result<User?> {
        return try {
            val document = usersCollection.document(id).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Salva o aggiorna il profilo utente.
     * Se l'ID salvato corrisponde all'utente corrente, aggiorna la cache.
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            if (user.id.isBlank()) {
                return Result.failure(IllegalArgumentException("L'ID utente non può essere vuoto"))
            }
            usersCollection.document(user.id).set(user).await()

            if (user.id == getCurrentUid()) {
                _currentUserProfile.value = user
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna solo campi specifici.
     */
    suspend fun updateFields(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(uid).update(updates).await()

            // Se stiamo aggiornando l'utente corrente, ricarichiamo la cache
            if (uid == getCurrentUid()) {
                loadAndCacheProfile(uid)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disattiva l'utente (Soft Delete).
     */
    suspend fun deactivateUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("stato", false).await()

            // Aggiorna la cache se è l'utente corrente
            if (userId == getCurrentUid()) {
                _currentUserProfile.value = _currentUserProfile.value?.copy(stato = false)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera tutti gli utenti registrati.
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pulisce la cache locale. Da chiamare obbligatoriamente al logout.
     */
    fun clearCache() {
        _currentUserProfile.value = null
    }


    /**
     * Segnala un utente per comportamento scorretto.
     *
     * @param reportedUserId L'ID dell'utente da segnalare.
     * @param reason La motivazione della segnalazione.
     * @return [Result] di tipo [Unit].
     */
    suspend fun reportUser(reportedUserId: String, reason: String): Result<Unit> {
        return try {
            val reporterId = getCurrentUid() ?: return Result.failure(Exception("Utente non autenticato"))

            val reportData = mapOf(
                "reportedUserId" to reportedUserId,
                "reporterUserId" to reporterId,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )

            db.collection("user_reports").add(reportData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}





