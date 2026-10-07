package com.example.micasaestucasa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.repository.ChatRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailedChatViewModel: ViewModel(){
    private val _uiState = MutableStateFlow(DetailedChatUiState())
    val uiState: StateFlow<DetailedChatUiState> = _uiState.asStateFlow()

    private var currentChatID : String ? = null
    val currentUserID = UsersRepository.currentUserProfile.value?.id
    private var otherUserID : String ? = null


    fun initChat(chatId: String?, otherUserId: String, casaId: String?, bookingId: String?){

        if(chatId == null){
            this.currentChatID = ChatRepository.getChatId(currentUserID!!, otherUserId)
        }else{
            this.currentChatID = chatId
        }

        this.otherUserID = otherUserId

        try{
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }

                val cid = currentChatID ?: return@launch
                val uid = currentUserID ?: return@launch

                UsersRepository.getUserById(otherUserId).onSuccess { user ->
                    _uiState.update { it.copy(otherUser = user) }
                }

                ChatRepository.getChatMetadata(cid, uid)
                    .onEach { preview ->
                        _uiState.update { it.copy(chatInfo = preview) }
                    }
                    .launchIn(viewModelScope)

                ChatRepository.getMessages(cid)
                    .onEach { messageList ->
                        _uiState.update { it.copy(
                            messages = messageList,
                            isLoading = false
                        ) }
                    }
                    .launchIn(viewModelScope)


                bookingId?.let { id ->
                    BookingRepository.getBookingById(id).onSuccess { booking ->
                        _uiState.update { it.copy(booking = booking) }
                    }
                }

                casaId?.let{
                    id-> val casa = CasaRepository.getCasaById(id)
                    _uiState.update { it.copy(house = casa) }
                }



            }
        }catch (e: Exception){
            _uiState.update { it.copy(errorMessage = e.message) }
        }

    }


    /**
     * Verifica se mostrare l'icona della recensione nella toolbar.
     */
    private suspend fun checkReviewEligibility() {
        val uid = currentUserID ?: return

        ReviewRepository.hasAlreadyReviewedUser(otherUserID!!, uid).onSuccess { alreadyReviewed ->
            _uiState.update { it.copy(
                canReview = true,
                hasAlreadyReviewed = alreadyReviewed
            ) }
        }
    }


    fun sendMessage(text: String) {
        if (text.isBlank() || currentUserID == null || otherUserID == null) return

        val cid = currentChatID ?: return
        val otherUser = _uiState.value.otherUser ?: return

        val me = UsersRepository.currentUserProfile.value

        viewModelScope.launch {
            // Se la chat non è ancora stata inizializzata su Firestore (chatInfo è null)
            if (_uiState.value.chatInfo == null && me != null) {
                ChatRepository.initializeChat(me, otherUser).onSuccess {
                    executeSend(cid, text)
                }
            } else if (_uiState.value.chatInfo != null) {
                // Chat già esistente, inviamo il messaggio direttamente
                executeSend(cid, text)
            } else {
                // Caso limite: me è null (sessione scaduta o profilo non caricato)
                _uiState.update { it.copy(errorMessage = "Errore: profilo utente non trovato") }
            }
        }
    }


    /**
     * Gestisce l'invio di un'immagine.
     * Carica il file su Storage e poi invia il messaggio con l'URL risultante.
     */
    fun sendImageMessage(uri: String) {
        val cid = currentChatID ?: return
        val uid = currentUserID ?: return
        val me = UsersRepository.currentUserProfile.value
        val otherUser = _uiState.value.otherUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            ChatRepository.uploadImage(cid, uri).onSuccess { imageUrl ->

                if (_uiState.value.chatInfo == null && me != null) {
                    ChatRepository.initializeChat(me, otherUser).onSuccess {
                        executeSendImage(cid, imageUrl)
                    }
                } else {
                    executeSendImage(cid, imageUrl)
                }
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Errore durante l'upload dell'immagine"
                ) }
            }
        }
    }

    private suspend fun executeSendImage(chatId: String, imageUrl: String) {
        val message = Message(
            senderId = currentUserID!!,
            text = "",
            url = imageUrl,
            type = "image"
        )
        ChatRepository.sendMessage(chatId, message)
    }


    private suspend fun executeSend(chatId: String, text: String) {
        val message = Message(
            senderId = currentUserID!!,
            text = text
        )
        ChatRepository.sendMessage(chatId, message)
    }


    fun deleteChat() {
        val cid = currentChatID ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            if (ChatRepository.deleteChat(cid)) {
                _uiState.update { it.copy(actionSuccess = true) }
            }
            _uiState.update { it.copy(isDeleting = false) }
        }
    }

    fun reportUser(reason: String) {
        val tid = otherUserID ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isReporting = true) }
            UsersRepository.reportUser(tid, reason).onSuccess {
                _uiState.update { it.copy(
                    isReporting = false,
                    userFeedback = "Utente segnalato con successo"
                ) }
            }
        }
    }


    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(userFeedback = null) }
    }

}




