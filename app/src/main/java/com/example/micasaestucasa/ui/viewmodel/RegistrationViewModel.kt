package com.example.micasaestucasa.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.User
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistrationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun register(name: String, surname: String, email: String, password: String, conPass: String) {
        if (!isInputValid(name, surname, email, password, conPass)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            AuthRepository.createUserWithEmailAndPassword(email, password)
                .onSuccess { firebaseUser ->
                    val newUser = User(
                        id = firebaseUser.uid,
                        name = name,
                        surname = surname,
                        email = email,
                        ruolo = "user",
                        stato = true
                    )

                    UsersRepository.saveUser(newUser)
                        .onSuccess {
                            _uiState.update { it.copy(isLoading = false, isSucess = true) }
                        }
                        .onFailure { exception ->
                            handleError(exception)
                        }
                }
                .onFailure { exception ->
                    handleError(exception)
                }
        }
    }

    /**
     * Gestione centralizzata degli errori (Auth + Firestore)
     */
    private fun handleError(e: Throwable) {
        val message = when (e) {
            is FirebaseAuthUserCollisionException -> "Questa email è già associata a un account."
            is FirebaseAuthWeakPasswordException -> "La password scelta è troppo debole."
            is FirebaseAuthInvalidCredentialsException -> "Formato email non valido."
            else -> {
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "Errore di permessi nel database durante la creazione del profilo."
                } else {
                    e.localizedMessage ?: "Errore durante la registrazione."
                }
            }
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private fun isInputValid(name: String, surname: String, email: String, password: String, conPass: String): Boolean {
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Compila tutti i campi") }
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "Formato email non valido") }
            return false
        }

        // 8 caratteri, una maiuscola e un numero
        val passwordRegex = "^(?=.*[A-Z])(?=.*[0-9]).{8,}".toRegex()
        if (!password.matches(passwordRegex)) {
            _uiState.update { it.copy(errorMessage = "La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero") }
            return false
        }

        if (conPass != password) {
            _uiState.update { it.copy(errorMessage = "Le password non corrispondono") }
            return false
        }

        return true
    }

    fun errorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

