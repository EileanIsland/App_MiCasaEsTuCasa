package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.ReportedUser
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdUsersUiState(
    val isLoading: Boolean = false,
    val reportedUsers: List<ReportedUser> = emptyList(),
    val count: Int = 0,
    val userFeedback: String? = null,
    val errorMessage: String? = null
)

class AdUsersViewMod : ViewModel() {
    private val _uiState = MutableStateFlow(AdUsersUiState())
    val uiState: StateFlow<AdUsersUiState> = _uiState.asStateFlow()

    init {
        loadReportedUsers()
    }

    fun loadReportedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            UsersRepository.getUsersWithReports().onSuccess { list ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        reportedUsers = list,
                        count = list.size
                    )
                }
            }.onFailure { exception ->
                handleError(exception, "Errore nel caricamento dati")
            }
        }
    }


    fun suspendUser(userId: String, days: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val suspendResult = UsersRepository.suspendUser(userId, days)
            val reportsResult = UsersRepository.markReportsAsHandled(userId)

            if (suspendResult.isSuccess && reportsResult.isSuccess) {
                loadReportedUsers()
                _uiState.update { it.copy(userFeedback = "Utente sospeso e segnalazioni archiviate") }
            } else {
                handleError(Exception("Errore durante l'operazione"), "Riprova più tardi")
            }
        }
    }

    fun dismissReports(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            UsersRepository.markReportsAsHandled(userId).onSuccess {
                loadReportedUsers()
                _uiState.update { it.copy(userFeedback = "Segnalazioni archiviate") }
            }.onFailure { e ->
                handleError(e as Exception, "Errore nell'archiviazione")
            }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val blockResult = UsersRepository.blockUser(userId)
            val reportsResult = UsersRepository.markReportsAsHandled(userId)

            if (blockResult.isSuccess && reportsResult.isSuccess) {
                loadReportedUsers()
                _uiState.update {
                    it.copy(userFeedback = "Utente bloccato e segnalazioni archiviate")
                }
            } else {
                val error = blockResult.exceptionOrNull() ?: reportsResult.exceptionOrNull() ?: Exception("Errore sconosciuto")
                handleError(error, "Errore durante il blocco dell'utente")
            }
        }
    }

    /**
     * Gestisce l'errore centralmente, ferma il caricamento e imposta il messaggio.
     */
    private fun handleError(t: Throwable, defaultMessage: String) {
        val finalMessage = if (t.message?.contains("PERMISSION_DENIED") == true) {
            "Non hai i permessi per eseguire questa operazione"
        } else {
            t.localizedMessage ?: defaultMessage
        }

        _uiState.update {
            it.copy(
                errorMessage = finalMessage,
                isLoading = false
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearUserFeedback() {
        _uiState.update { it.copy(userFeedback = null) }
    }
}



