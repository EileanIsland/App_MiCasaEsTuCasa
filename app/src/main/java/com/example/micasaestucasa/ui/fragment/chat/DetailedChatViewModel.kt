package com.example.micasaestucasa.ui.fragment.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.CasaRepository
import com.example.micasaestucasa.data.repository.ChatRepository
import com.example.micasaestucasa.data.repository.UsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailedChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailedChatUiState())
    val uiState: StateFlow<DetailedChatUiState> = _uiState.asStateFlow()

    private var _currentChatID: String? = null
    val currentChatID: String? get() = _currentChatID

    private var _otherUserID: String? = null
    val otherUserID: String? get() = _otherUserID

    private var isContextChecked = false

    val currentUserID: String? get() = UsersRepository.getCurrentUid()


    /**
     * Inizializza la chat caricando messaggi, profili e dettagli contesto.
     */
    fun initChat(chatId: String?, otherUserId: String?, casaId: String?, bookingId: String?) {
        if (chatId.isNullOrEmpty() && otherUserId.isNullOrEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val uid = currentUserID ?: return@launch

            val cid = chatId ?: ChatRepository.getChatId(uid, otherUserId!!)
            this@DetailedChatViewModel._currentChatID = cid
            this@DetailedChatViewModel._otherUserID = otherUserId

            otherUserId?.let {
                loadOtherUser(it)
            }

            ChatRepository.getChatMetadata(cid, uid)
                .catch { e -> handleError(e, "Errore metadati chat") }
                .onEach { preview ->
                    _uiState.update { it.copy(chatInfo = preview) }

                    if (this@DetailedChatViewModel._otherUserID == null && preview != null) {
                        val extractedId = preview.otherUserId
                        this@DetailedChatViewModel._otherUserID = extractedId
                        loadOtherUser(extractedId)
                    }
                }
                .launchIn(viewModelScope)

            if (bookingId != null) {
                BookingRepository.getBookingById(bookingId).onSuccess { b ->
                    val isHost = b?.idHost == uid
                    _uiState.update { it.copy(booking = b, isHost = isHost) }
                    b?.idCasa?.let { loadHouseData(it) }
                }
            } else if (casaId != null) {
                CasaRepository.getCasaById(casaId).onSuccess { casa ->
                    val isHost = casa?.proprietarioId == uid
                    _uiState.update { it.copy(house = casa, isHost = isHost) }
                }
            }

            ChatRepository.getMessages(cid, uid)
                .catch { e -> handleError(e, "Errore caricamento messaggi") }
                .onEach { messageList ->
                    _uiState.update { it.copy(messages = messageList, isLoading = false) }
                    if (messageList.isNotEmpty() && !isContextChecked) {
                        checkAndSendContext(cid, uid)
                        isContextChecked = true
                    }
                    syncBookingMetadata(messageList)

                    if (_uiState.value.house == null && _uiState.value.booking == null) {
                        loadCardDataFromMessages(messageList)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun loadOtherUser(userId: String) {
        viewModelScope.launch {
            UsersRepository.getUserById(userId)
                .onSuccess { user ->
                    _uiState.update { it.copy(otherUser = user) }
                }
                .onFailure { e -> handleError(e, "Errore profilo interlocutore") }
        }
    }

    /**
     * Identifica i messaggi di tipo 'booking_summary' e ne scarica i dati completi.
     */
    private fun syncBookingMetadata(messages: List<Message>) {
        val bookingIds = messages
            .filter { it.type == "booking_summary" }
            .mapNotNull { it.contextId }
            .distinct()

        if (bookingIds.isEmpty()) return

        viewModelScope.launch {
            val currentMap = _uiState.value.bookingsMap.toMutableMap()
            var hasChanged = false

            bookingIds.forEach { id ->
                if (!currentMap.containsKey(id)) {
                    BookingRepository.getBookingById(id).onSuccess { b ->
                        b?.let {
                            currentMap[id] = it
                            hasChanged = true
                        }
                    }
                }
            }

            if (hasChanged) {
                _uiState.update { it.copy(bookingsMap = currentMap) }
            }
        }
    }

    private suspend fun loadCardDataFromMessages(messages: List<Message>) {
        val lastContextMsg = messages.lastOrNull { it.type == "context" || it.type == "booking_summary" } ?: return
        val contextId = lastContextMsg.contextId ?: return

        if(lastContextMsg.contextType == "BOOKING" || lastContextMsg.type == "booking_summary"){
            BookingRepository.getBookingById(contextId).onSuccess { b ->
                val isHost = b?.idHost == currentUserID
                _uiState.update { it.copy(booking = b, isHost = isHost) }
                b?.idCasa?.let{ loadHouseData(it)}
            }
        } else {
            CasaRepository.getCasaById(contextId).onSuccess { casa ->
                val isHost = casa?.proprietarioId == currentUserID
                _uiState.update { it.copy(house = casa, isHost = isHost) }
            }
        }
    }

    private suspend fun loadHouseData(id: String) {
        CasaRepository.getCasaById(id).onSuccess { casa ->
            _uiState.update { it.copy(house = casa) }
        }
    }

    fun sendMessage(text: String) {
        val uid = currentUserID ?: return
        val cid = currentChatID ?: return
        if (text.isBlank()) return
        this.isContextChecked = false

        val state = _uiState.value
        val me = UsersRepository.currentUserProfile.value
        val other = state.otherUser

        viewModelScope.launch {
            if (_uiState.value.chatInfo == null && me != null && other != null) {
                val contextId = state.booking?.idBooking ?: state.house?.id
                val contextType = if (state.booking != null) "BOOKING" else if (state.house != null) "HOUSE" else null
                val contextTitle = state.house?.titolo ?: "Richiesta informazioni"
                val contextUrl = state.house?.immagini?.firstOrNull() ?: ""

                ChatRepository.initializeChat(
                    currentUser = me,
                    otherUser = other,
                    contextId = contextId,
                    contextType = contextType,
                    contextTitle = contextTitle,
                    contextImageUrl = contextUrl
                ).onSuccess {
                    executeSend(cid, text, uid)
                }.onFailure { e -> handleError(e, "Impossibile avviare la conversazione") }
            } else {
                executeSend(cid, text, uid)
            }
        }
    }

    private fun checkAndSendContext(chatId: String, senderId: String) {
        if(isContextChecked) return

        val state = _uiState.value

        val (currentContextId, currentContextType) = when {
            state.booking != null -> state.booking.idBooking to "BOOKING"
            state.house != null -> state.house.id to "HOUSE"
            else -> null to null
        }
        if (currentContextId == null || currentContextType == null) return

        val isAlreadyInMessages = state.messages.any { msg ->
            (msg.type == "context" || msg.type == "booking_summary") && msg.contextId == currentContextId
        }

        if (isAlreadyInMessages) {
            isContextChecked = true
            return
        }

        isContextChecked = true

        viewModelScope.launch {
            try {
                ChatRepository.sendContextMessage(
                    chatId = chatId,
                    senderId = senderId,
                    contextId = currentContextId,
                    contextType = currentContextType,
                    contextTitle = state.house?.titolo ?: "Informazioni casa",
                    contextImageUrl = state.house?.immagini?.firstOrNull()
                )
            } catch (e: Exception) {
                isContextChecked = false
                handleError(e, "Invio messaggio di contesto fallito")
            }
        }
    }

    private suspend fun executeSend(chatId: String, text: String, senderId: String) {
        val message = Message(senderId = senderId, text = text)
        ChatRepository.sendMessage(chatId, message)
    }

    fun sendImageMessage(uri: String) {
        val cid = currentChatID ?: return
        val uid = currentUserID ?: return
        val me = UsersRepository.currentUserProfile.value
        val other = _uiState.value.otherUser

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            ChatRepository.uploadImage(cid, uri).onSuccess { imageUrl ->
                if (_uiState.value.chatInfo == null && me != null && other != null) {
                    ChatRepository.initializeChat(me, other).onSuccess {
                        executeSendImage(cid, imageUrl, uid)
                    }
                } else {
                    executeSendImage(cid, imageUrl, uid)
                }
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                handleError(e, "Errore durante l'invio dell'immagine")
            }
        }
    }

    private suspend fun executeSendImage(chatId: String, imageUrl: String, senderId: String) {
        val message = Message(senderId = senderId, text = "", url = imageUrl, type = "image")
        ChatRepository.sendMessage(chatId, message)
    }

    private fun handleError(e: Throwable, context: String) {
        val message = if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Accesso negato: non hai i permessi per questa operazione."
        } else {
            "$context: ${e.localizedMessage ?: "Errore imprevisto"}"
        }
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }

    fun deleteChat() {
        val cid = currentChatID ?: return
        viewModelScope.launch {
            if (ChatRepository.deleteChat(cid)) {
                _uiState.update { it.copy(actionSuccess = true) }
            } else {
                _uiState.update { it.copy(errorMessage = "Errore durante l'eliminazione") }
            }
        }
    }

    fun reportUser(reason: String) {
        val tid = otherUserID ?: return
        val currentUser = UsersRepository.currentUserProfile.value ?: return
        viewModelScope.launch {
            UsersRepository.reportUser(tid, currentUser.name, reason)
                .onSuccess { _uiState.update { it.copy(userFeedback = "Utente segnalato") } }
                .onFailure { e -> handleError(e, "Segnalazione fallita") }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearFeedback() = _uiState.update { it.copy(userFeedback = null) }
}


