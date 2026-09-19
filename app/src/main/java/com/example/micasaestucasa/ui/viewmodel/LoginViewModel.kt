package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.AuthRepository
import com.example.micasaestucasa.data.repository.UsersRepository
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

            try{
                val firebaseUser = AuthRepository.signInWithEmailAndPassword(email, password)
                if(firebaseUser != null){
                    //login ok ha avuto successo
                    val userProfile = UsersRepository.getUserById(firebaseUser.uid)
                        .onSuccess{ user ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isSuccess = true,
                                    isAdmin = user?.ruolo == "admin"
                                )
                            }
                        }.onFailure{error->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Errre nel caricamento del profilo utente"
                                )}
                        }

                }else{
                    //login fallito
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Credenziali non valide"
                        )
                    }
                }
            }catch(e: Exception){
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Errore di connessione")
                }
            }
        }
    }

    private fun isEmailValid(email: String): Boolean{
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun errorShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }


}