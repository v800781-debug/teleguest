package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Chat
import com.example.data.model.ChatType
import com.example.data.model.GuestModeSettings
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import com.example.data.repository.TelegramRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChatFilterTab(val title: String) {
    ALL("Все"),
    PERSONAL("Личные"),
    GROUPS("Группы"),
    CHANNELS("Каналы"),
    BOTS("Боты")
}

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TelegramRepository

    val guestSettings: StateFlow<GuestModeSettings>
    val userProfile: StateFlow<UserProfile>

    private val _selectedTab = MutableStateFlow(ChatFilterTab.ALL)
    val selectedTab = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    private val _isPinUnlockDialogOpen = MutableStateFlow(false)
    val isPinUnlockDialogOpen = _isPinUnlockDialogOpen.asStateFlow()

    private val _isGuestSettingsDialogOpen = MutableStateFlow(false)
    val isGuestSettingsDialogOpen = _isGuestSettingsDialogOpen.asStateFlow()

    private val _isNewChatDialogOpen = MutableStateFlow(false)
    val isNewChatDialogOpen = _isNewChatDialogOpen.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<Message?>(null)
    val replyingToMessage = _replyingToMessage.asStateFlow()

    private val _statusNotification = MutableStateFlow<String?>(null)
    val statusNotification = _statusNotification.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TelegramRepository(database.chatDao(), database.messageDao())
        guestSettings = repository.guestSettings
        userProfile = repository.userProfile

        viewModelScope.launch {
            repository.populateInitialSeedDataIfEmpty()
        }
    }

    // Filtered chats flow based on search, tab, and guest mode
    val filteredChats: StateFlow<List<Chat>> = combine(
        repository.chatsStream,
        _selectedTab,
        _searchQuery
    ) { chats, tab, query ->
        chats.filter { chat ->
            val matchesTab = when (tab) {
                ChatFilterTab.ALL -> true
                ChatFilterTab.PERSONAL -> chat.type == ChatType.DIRECT || chat.type == ChatType.SAVED_MESSAGES
                ChatFilterTab.GROUPS -> chat.type == ChatType.GROUP
                ChatFilterTab.CHANNELS -> chat.type == ChatType.CHANNEL
                ChatFilterTab.BOTS -> chat.type == ChatType.BOT
            }
            val matchesQuery = query.isBlank() ||
                    chat.title.contains(query, ignoreCase = true) ||
                    (chat.username?.contains(query, ignoreCase = true) == true) ||
                    chat.lastMessage.contains(query, ignoreCase = true)

            matchesTab && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active chat object
    val activeChat: StateFlow<Chat?> = _activeChatId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getChatFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Messages for the active chat
    val activeMessages: StateFlow<List<Message>> = _activeChatId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getMessagesForChat(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: ChatFilterTab) {
        _selectedTab.value = tab
    }

    fun loginWithPhone(phone: String, name: String, username: String) {
        val finalName = name.ifBlank { "Пользователь" }
        repository.loginWithPhone(phone, finalName, username)
        _statusNotification.value = "Добро пожаловать в TeleGuest, $finalName!"
    }

    fun loginAsGuest(nickname: String = "Гость") {
        val cleanNick = nickname.ifBlank { "Гость" }
        repository.loginAsGuest(cleanNick)
        _statusNotification.value = "🛡️ Вы вошли как $cleanNick (Гостевой режим активен)"
    }

    fun logout() {
        closeChat()
        repository.logout()
        _statusNotification.value = "Вы вышли из профиля"
    }

    fun updateUserProfile(name: String, username: String, bio: String) {
        repository.updateUserProfile(name, username, bio)
        _statusNotification.value = "Профиль обновлен"
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openChat(chatId: String) {
        _activeChatId.value = chatId
    }

    fun closeChat() {
        _activeChatId.value = null
        _replyingToMessage.value = null
    }

    fun setReplyingTo(message: Message?) {
        _replyingToMessage.value = message
    }

    fun enterGuestMode() {
        viewModelScope.launch {
            repository.enterGuestMode()
            // If active chat is private, close it
            val active = activeChat.value
            if (active?.isPrivateToOwner == true) {
                closeChat()
            }
            _statusNotification.value = "🛡️ Гостевой режим включен. Личные чаты скрыты."
        }
    }

    fun requestExitGuestMode() {
        _isPinUnlockDialogOpen.value = true
    }

    fun verifyAndExitGuestMode(pin: String): Boolean {
        var success = false
        if (pin == guestSettings.value.pinCode) {
            viewModelScope.launch {
                repository.exitGuestMode(pin)
                _isPinUnlockDialogOpen.value = false
                _statusNotification.value = "🔓 Гостевой режим отключен. Доступ к аккаунту восстановлен."
            }
            success = true
        }
        return success
    }

    fun openPinUnlockDialog(open: Boolean) {
        _isPinUnlockDialogOpen.value = open
    }

    fun openGuestSettingsDialog(open: Boolean) {
        _isGuestSettingsDialogOpen.value = open
    }

    fun openNewChatDialog(open: Boolean) {
        _isNewChatDialogOpen.value = open
    }

    fun updateGuestSettings(newSettings: GuestModeSettings) {
        viewModelScope.launch {
            repository.updateGuestSettings(newSettings)
            _isGuestSettingsDialogOpen.value = false
            _statusNotification.value = "Настройки гостевого режима сохранены"
        }
    }

    fun clearGuestDataNow() {
        viewModelScope.launch {
            repository.clearGuestData()
            _statusNotification.value = "Временные данные и переписки гостя удалены"
        }
    }

    fun sendMessage(
        text: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null,
        voiceDurationSeconds: Int = 0
    ) {
        val chatId = _activeChatId.value ?: return
        if (text.isBlank() && type == MessageType.TEXT) return

        if (guestSettings.value.isGuestModeActive && !guestSettings.value.allowSendingMessages) {
            _statusNotification.value = "🔒 Отправка сообщений заблокирована в гостевом режиме"
            return
        }

        viewModelScope.launch {
            val reply = _replyingToMessage.value
            repository.sendMessage(
                chatId = chatId,
                text = text,
                type = type,
                mediaUrl = mediaUrl,
                voiceDurationSeconds = voiceDurationSeconds,
                replyTo = reply
            )
            _replyingToMessage.value = null
        }
    }

    fun sendVoiceMessage(durationSeconds: Int) {
        sendMessage(
            text = "Голосовое сообщение",
            type = MessageType.VOICE,
            voiceDurationSeconds = durationSeconds
        )
    }

    fun sendSticker(stickerText: String) {
        sendMessage(
            text = stickerText,
            type = MessageType.STICKER
        )
    }

    fun toggleReaction(message: Message, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(message, emoji)
        }
    }

    fun togglePinChat(chat: Chat) {
        viewModelScope.launch {
            repository.togglePin(chat)
        }
    }

    fun toggleMuteChat(chat: Chat) {
        viewModelScope.launch {
            repository.toggleMute(chat)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_activeChatId.value == chatId) {
                closeChat()
            }
            _statusNotification.value = "Чат удален"
        }
    }

    fun createNewChat(
        title: String,
        username: String,
        type: ChatType,
        isPrivate: Boolean,
        bio: String
    ) {
        viewModelScope.launch {
            val chat = repository.createNewChat(title, username, type, isPrivate, bio)
            _isNewChatDialogOpen.value = false
            openChat(chat.id)
            _statusNotification.value = "${if (type == ChatType.CHANNEL) "Канал" else "Чат"} «$title» создан"
        }
    }

    fun clearNotification() {
        _statusNotification.value = null
    }
}
