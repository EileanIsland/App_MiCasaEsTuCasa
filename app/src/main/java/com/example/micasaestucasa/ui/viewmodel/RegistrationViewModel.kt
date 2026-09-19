package com.example.micasaestucasa.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.User
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.UsersRepository
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

            try {
                val firebaseUser = AuthRepository.createUserWithEmailAndPassword(email, password)

                if (firebaseUser != null) {
                    val newUser = User(
                        id = firebaseUser.uid,
                        name = name,
                        surname = surname,
                        email = email,
                        ruolo = "user",
                        stato = true
                    )

                    //Salvataggio su Firestore usando Result<Unit>
                    UsersRepository.saveUser(newUser)
                        .onSuccess {
                            _uiState.update { it.copy(isLoading = false, isSucess = true) }
                        }
                        .onFailure { exception ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Errore database: ${exception.localizedMessage}"
                                )
                            }
                        }

                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Impossibile creare l'account")
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Errore imprevisto")
                }
            }
        }
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

        val passwordRegex = "^(?=.*[A-Z])(?=.*[0-9]).{8,}".toRegex()

        if (!password.matches(passwordRegex)) {
            _uiState.update { it.copy(errorMessage = "La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero") }
            return false
        }

        if(conPass != password){
            _uiState.update { it.copy(errorMessage = "Le password non corrispondono") }
            return false
        }

        return true
    }

    fun errorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}


