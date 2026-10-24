package com.example.micasaestucasa.ui.fragment.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel: ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Inserisci email e password") }
            return
        }

        if(!isEmailValid(email)){
            _uiState.update { it.copy(errorMessage = "Email non valida") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                AuthRepository.signInWithEmailAndPassword(email, password)
                    .onSuccess { firebaseUser ->
                        loadUserProfile(firebaseUser.uid)
                    }
                    .onFailure { error->
                        handleError(error)
                    }
        }
    }

    private suspend fun loadUserProfile(uid: String) {
        UsersRepository.getUserById(uid)
            .onSuccess { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            isAdmin = user.ruolo == "admin"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Profilo utente non trovato."
                        )
                    }
                }
            }
            .onFailure { error ->
                handleError(error)
            }
    }


    private fun handleError(e: Throwable) {
        val message = when (e) {
            is FirebaseAuthInvalidUserException -> "L'account non esiste."
            is FirebaseAuthInvalidCredentialsException -> "Email o password errati."
            else -> {
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "Errore di accesso al database. Controlla la tua connessione."
                } else {
                    e.localizedMessage ?: "Si è verificato un errore imprevisto."
                }
            }
        }

        _uiState.update {
            it.copy(isLoading = false, errorMessage = message)
        }
    }

    private fun isEmailValid(email: String): Boolean{
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun errorShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

}



