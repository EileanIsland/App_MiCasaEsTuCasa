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

    fun toggleUserExpansion(userId: String) {
        _uiState.update { state ->
            val newList = state.reportedUsers.map {
                if (it.user.id == userId) it.copy(isExpanded = !it.isExpanded)
                else it
            }
            state.copy(reportedUsers = newList)
        }
    }

    fun suspendUser(userId: String, days: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            UsersRepository.suspendUser(userId, days).onSuccess {
                loadReportedUsers()
                _uiState.update { it.copy(userFeedback = "Utente sospeso per $days giorni") }
            }.onFailure { exception ->
                handleError(exception, "Errore nel sospendere l'utente")
            }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            UsersRepository.blockUser(userId).onSuccess {
                loadReportedUsers()
                _uiState.update { it.copy(userFeedback = "Utente bloccato") }
            }.onFailure { exception ->
                handleError(exception, "Errore nel bloccare l'utente")
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



