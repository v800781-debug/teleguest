package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.MessageDao
import com.example.data.model.Chat
import com.example.data.model.ChatType
import com.example.data.model.GuestModeSettings
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TelegramRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    private val _guestSettings = MutableStateFlow(GuestModeSettings())
    val guestSettings = _guestSettings.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile(isLoggedIn = false))
    val userProfile = _userProfile.asStateFlow()

    // Real-time filtered chats based on whether guest mode is active
    val chatsStream: Flow<List<Chat>> = _guestSettings.flatMapLatest { settings ->
        if (settings.isGuestModeActive && settings.hidePersonalChats) {
            chatDao.getPublicChatsForGuest()
        } else {
            chatDao.getAllChats()
        }
    }

    fun loginWithPhone(phone: String, name: String, username: String) {
        val cleanUsername = if (username.startsWith("@")) username else "@$username"
        _userProfile.value = UserProfile(
            name = name.ifBlank { "Пользователь" },
            username = cleanUsername.ifBlank { "@user" },
            phone = phone.ifBlank { "+7 (999) 000-00-00" },
            bio = "Пользуюсь TeleGuest ✨",
            isPremium = false,
            isLoggedIn = true,
            isGuestAccount = false
        )
        // Ensure guest mode is off when logging in as real user
        _guestSettings.value = _guestSettings.value.copy(isGuestModeActive = false)
    }

    fun loginAsGuest(nickname: String = "Гость") {
        val cleanNick = nickname.ifBlank { "Гость" }
        _userProfile.value = UserProfile(
            name = cleanNick,
            username = "@${cleanNick.lowercase().replace(" ", "_")}",
            phone = "Гостевой сеанс",
            bio = "Гостевой аккаунт TeleGuest 🛡️",
            isPremium = false,
            isLoggedIn = true,
            isGuestAccount = true
        )
        // Automatically activate Guest Mode
        _guestSettings.value = _guestSettings.value.copy(
            isGuestModeActive = true,
            guestNickname = cleanNick,
            sessionStartTime = System.currentTimeMillis()
        )
    }

    fun logout() {
        _userProfile.value = UserProfile(isLoggedIn = false)
        _guestSettings.value = _guestSettings.value.copy(isGuestModeActive = false)
    }

    fun updateUserProfile(name: String, username: String, bio: String) {
        _userProfile.value = _userProfile.value.copy(
            name = name,
            username = if (username.startsWith("@")) username else "@$username",
            bio = bio
        )
    }

    fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId)
    }

    fun getChatFlow(chatId: String): Flow<Chat?> {
        return chatDao.getChatFlowById(chatId)
    }

    suspend fun getChatById(chatId: String): Chat? {
        return chatDao.getChatById(chatId)
    }

    suspend fun updateGuestSettings(newSettings: GuestModeSettings) {
        _guestSettings.value = newSettings
    }

    suspend fun enterGuestMode() {
        _guestSettings.value = _guestSettings.value.copy(
            isGuestModeActive = true,
            sessionStartTime = System.currentTimeMillis()
        )
    }

    suspend fun exitGuestMode(verifiedPin: String): Boolean {
        if (verifiedPin == _guestSettings.value.pinCode) {
            if (_guestSettings.value.clearHistoryOnExit) {
                // Clear any messages or chats created during guest mode
                messageDao.deleteGuestMessages()
                chatDao.deleteGuestChats()
            }
            _guestSettings.value = _guestSettings.value.copy(
                isGuestModeActive = false,
                sessionStartTime = 0L
            )
            return true
        }
        return false
    }

    suspend fun clearGuestData() {
        messageDao.deleteGuestMessages()
        chatDao.deleteGuestChats()
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null,
        voiceDurationSeconds: Int = 0,
        replyTo: Message? = null
    ) {
        val isGuest = _guestSettings.value.isGuestModeActive
        val senderName = if (isGuest) _guestSettings.value.guestNickname else _userProfile.value.name
        val now = System.currentTimeMillis()
        val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))

        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderName = senderName,
            text = text,
            timestamp = now,
            timeFormatted = timeFormatted,
            isFromMe = true,
            isRead = true,
            type = type,
            mediaUrl = mediaUrl,
            voiceDurationSeconds = voiceDurationSeconds,
            replyToText = replyTo?.text,
            replyToSender = replyTo?.senderName,
            isGuestMessage = isGuest
        )

        messageDao.insertMessage(message)

        // Update chat's last message
        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            val previewText = when (type) {
                MessageType.VOICE -> "🎤 Голосовое сообщение (${voiceDurationSeconds}с)"
                MessageType.PHOTO -> "📷 Фотография"
                MessageType.STICKER -> "🌟 Стикер $text"
                else -> text
            }
            chatDao.updateChat(
                chat.copy(
                    lastMessage = previewText,
                    lastMessageTime = timeFormatted,
                    lastMessageTimestamp = now
                )
            )

            // Auto-reply bot or interactive response simulation
            if (chat.type == ChatType.BOT || chat.type == ChatType.DIRECT) {
                triggerBotOrFriendReply(chat, text)
            }
        }
    }

    private fun triggerBotOrFriendReply(chat: Chat, userMessage: String) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(1200) // Realistic typing delay

            val now = System.currentTimeMillis()
            val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))

            val replyText = when {
                chat.type == ChatType.BOT -> {
                    when {
                        userMessage.startsWith("/start") ->
                            "👋 Приветствую в TeleGuest! Я умный ассистент мессенджера. Чем могу помочь? Доступны команды: /guest, /crypto, /joke, /help."
                        userMessage.startsWith("/guest") ->
                            "🛡️ В гостевом режиме TeleGuest ваши личные данные надежно защищены PIN-кодом, а приватные диалоги скрыты."
                        userMessage.startsWith("/joke") ->
                            "😄 — Почему программисты путают Хэллоуин и Рождество?\n— Потому что 31 OCT = 25 DEC!"
                        userMessage.startsWith("/crypto") ->
                            "💎 TON: $6.42 (+4.8%)\n🚀 BTC: $96,400\n⚡ TeleGuest Stars: 100⭐ = $1.99"
                        userMessage.startsWith("/help") ->
                            "ℹ️ Возможности TeleGuest:\n• Гостевой режим с PIN-блокировкой\n• Разделение личных и публичных чатов\n• Вход по номеру или быстрый гость"
                        else ->
                            "🤖 TeleGuest Bot получил ваш запрос: «$userMessage». Всё работает мгновенно и безопасно! ⚡"
                    }
                }
                chat.id == "alice_direct" -> {
                    "Привет! Рада тебя слышать 😊 Как твои дела?"
                }
                else -> {
                    "Отлично, сообщение принято! 👍"
                }
            }

            val replyMessage = Message(
                id = UUID.randomUUID().toString(),
                chatId = chat.id,
                senderName = chat.title,
                text = replyText,
                timestamp = now,
                timeFormatted = timeFormatted,
                isFromMe = false,
                isRead = true,
                type = MessageType.TEXT,
                isGuestMessage = _guestSettings.value.isGuestModeActive
            )

            messageDao.insertMessage(replyMessage)

            chatDao.updateChat(
                chat.copy(
                    lastMessage = replyText,
                    lastMessageTime = timeFormatted,
                    lastMessageTimestamp = now
                )
            )
        }
    }

    suspend fun togglePin(chat: Chat) {
        chatDao.updateChat(chat.copy(isPinned = !chat.isPinned))
    }

    suspend fun toggleMute(chat: Chat) {
        chatDao.updateChat(chat.copy(isMuted = !chat.isMuted))
    }

    suspend fun deleteChat(chatId: String) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.deleteChatById(chatId)
    }

    suspend fun toggleReaction(message: Message, emoji: String) {
        val currentReactions = message.reactions.split(",").filter { it.isNotBlank() }.toMutableList()
        if (currentReactions.contains(emoji)) {
            currentReactions.remove(emoji)
        } else {
            currentReactions.add(emoji)
        }
        messageDao.updateMessage(message.copy(reactions = currentReactions.joinToString(",")))
    }

    suspend fun createNewChat(
        title: String,
        username: String,
        type: ChatType,
        isPrivate: Boolean,
        bio: String
    ): Chat {
        val id = UUID.randomUUID().toString()
        val colors = listOf("#2AABEE", "#E56555", "#8E44AD", "#27AE60", "#F39C12", "#D35400", "#16A085")
        val color = colors.random()
        val isGuest = _guestSettings.value.isGuestModeActive

        val chat = Chat(
            id = id,
            title = title,
            username = "@${username.removePrefix("@")}",
            type = type,
            lastMessage = "Чат создан",
            lastMessageTime = "сейчас",
            lastMessageTimestamp = System.currentTimeMillis(),
            avatarColorHex = color,
            memberCount = if (type == ChatType.GROUP) 2 else if (type == ChatType.CHANNEL) 1 else 0,
            bio = bio,
            isPrivateToOwner = isPrivate,
            isCreatedByGuest = isGuest
        )

        chatDao.insertChat(chat)

        val systemMsg = Message(
            id = UUID.randomUUID().toString(),
            chatId = id,
            senderName = "TeleGuest",
            text = if (type == ChatType.CHANNEL) "Канал «$title» создан" else "Беседа «$title» начата",
            timestamp = System.currentTimeMillis(),
            timeFormatted = "сейчас",
            isFromMe = false,
            isRead = true,
            type = MessageType.SYSTEM,
            isGuestMessage = isGuest
        )
        messageDao.insertMessage(systemMsg)

        return chat
    }

    suspend fun populateInitialSeedDataIfEmpty() {
        // If chats exist, ensure channels/bots are updated to TeleGuest branding
        if (chatDao.getChatCount() > 0) {
            val existingNews = chatDao.getChatById("telegram_news")
            if (existingNews != null) {
                chatDao.updateChat(
                    existingNews.copy(
                        title = "TeleGuest Новости",
                        username = "@teleguest_news",
                        bio = "Официальный канал с новостями мессенджера TeleGuest.",
                        lastMessage = "Новое обновление TeleGuest: Гостевой режим, приватность и быстрый вход!"
                    )
                )
            }
            val existingBot = chatDao.getChatById("assistant_bot")
            if (existingBot != null) {
                chatDao.updateChat(
                    existingBot.copy(
                        title = "TeleGuest Assistant",
                        username = "@teleguest_bot",
                        bio = "Умный помощник TeleGuest для защиты данных и навигации."
                    )
                )
            }
            val existingDurov = chatDao.getChatById("durov_channel")
            if (existingDurov != null) {
                chatDao.updateChat(
                    existingDurov.copy(
                        title = "TeleGuest Official",
                        username = "@teleguest_app",
                        bio = "Официальный блог разработчиков TeleGuest.",
                        lastMessage = "TeleGuest теперь доступен для всех! Приватный гостевой режим готов. 🚀"
                    )
                )
            }
            return
        }

        val initialChats = listOf(
            Chat(
                id = "saved_messages",
                title = "Избранное",
                username = null,
                type = ChatType.SAVED_MESSAGES,
                lastMessage = "🔑 Заметки и сохраненные файлы",
                lastMessageTime = "11:45",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                avatarColorHex = "#2AABEE",
                isPinned = true,
                isPrivateToOwner = true // PRIVATE: Hide in Guest Mode!
            ),
            Chat(
                id = "teleguest_news",
                title = "TeleGuest Новости",
                username = "@teleguest_news",
                type = ChatType.CHANNEL,
                lastMessage = "Новое обновление TeleGuest: Гостевой режим, приватность и быстрый вход!",
                lastMessageTime = "09:30",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                isVerified = true,
                avatarColorHex = "#2AABEE",
                memberCount = 1450000,
                bio = "Официальный канал с новостями мессенджера TeleGuest.",
                isPrivateToOwner = false // PUBLIC: Visible in Guest Mode
            ),
            Chat(
                id = "durov_channel",
                title = "TeleGuest Official",
                username = "@teleguest_app",
                type = ChatType.CHANNEL,
                lastMessage = "TeleGuest теперь доступен для всех! Приватный гостевой режим готов. 🚀",
                lastMessageTime = "10:12",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
                unreadCount = 1,
                isPinned = true,
                isVerified = true,
                avatarColorHex = "#1E88E5",
                memberCount = 890000,
                bio = "Официальный блог разработчиков TeleGuest.",
                isPrivateToOwner = false // PUBLIC: Visible in Guest Mode
            ),
            Chat(
                id = "assistant_bot",
                title = "TeleGuest Assistant",
                username = "@teleguest_bot",
                type = ChatType.BOT,
                lastMessage = "👋 Приветствую! Используйте /guest или /joke для теста.",
                lastMessageTime = "Вчера",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                avatarColorHex = "#8E24AA",
                bio = "Умный помощник TeleGuest для защиты данных и навигации.",
                isPrivateToOwner = false // BOT: Available in Guest Mode
            ),
            Chat(
                id = "alice_direct",
                title = "Алиса Смирнова",
                username = "@alice_s",
                type = ChatType.DIRECT,
                lastMessage = "Договорились, встретимся в 19:00 в кофейне ☕",
                lastMessageTime = "12:10",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 5,
                unreadCount = 1,
                isOnline = true,
                avatarColorHex = "#E91E63",
                bio = "Product Designer & Travel lover ✈️",
                isPrivateToOwner = true // PRIVATE: Hide in Guest Mode!
            ),
            Chat(
                id = "secret_work_group",
                title = "💼 Финансовый отчет (Secret)",
                username = null,
                type = ChatType.GROUP,
                lastMessage = "Михаил: Отправил сводку за 4 квартал в PDF",
                lastMessageTime = "08:15",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
                avatarColorHex = "#D32F2F",
                memberCount = 5,
                bio = "Закрытая рабочая группа по инвестициям.",
                isPrivateToOwner = true // PRIVATE: Hide in Guest Mode!
            ),
            Chat(
                id = "dev_community",
                title = "Android & Kotlin Devs",
                username = "@kotlin_android_chat",
                type = ChatType.GROUP,
                lastMessage = "Кто уже пробовал Jetpack Compose в проде?",
                lastMessageTime = "Вчера",
                lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 26,
                avatarColorHex = "#388E3C",
                memberCount = 2450,
                bio = "Крупнейшее сообщество мобильных разработчиков.",
                isPrivateToOwner = false // PUBLIC: Visible in Guest Mode
            )
        )

        chatDao.insertChats(initialChats)

        // Seed messages
        val initialMessages = listOf(
            Message(
                id = "m1",
                chatId = "saved_messages",
                senderName = "Избранное",
                text = "🔑 Мои важные заметки и ссылки\nДоступны только вам.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                timeFormatted = "11:45",
                isFromMe = true,
                isRead = true,
                type = MessageType.TEXT
            ),
            Message(
                id = "m2",
                chatId = "teleguest_news",
                senderName = "TeleGuest Новости",
                text = "⚡ Встречайте TeleGuest! Главные особенности:\n• Полный гостевой режим для безопасного показа телефона друзьям\n• Вход по номеру или мгновенный гостевой вход\n• Защита личных чатов PIN-кодом",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                timeFormatted = "09:30",
                isFromMe = false,
                isRead = true,
                type = MessageType.TEXT,
                reactions = "🔥,❤️,👍",
                viewsCount = "1.4M"
            ),
            Message(
                id = "m3",
                chatId = "durov_channel",
                senderName = "TeleGuest Official",
                text = "TeleGuest теперь доступен для всех! Приватный гостевой режим готов. 🚀\nСпасибо нашему комьюнити за поддержку!",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
                timeFormatted = "10:12",
                isFromMe = false,
                isRead = true,
                type = MessageType.TEXT,
                reactions = "🚀,👍",
                viewsCount = "890K"
            ),
            Message(
                id = "m4",
                chatId = "alice_direct",
                senderName = "Алиса Смирнова",
                text = "Привет! Ты сегодня свободен вечером?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 20,
                timeFormatted = "11:58",
                isFromMe = false,
                isRead = true,
                type = MessageType.TEXT
            ),
            Message(
                id = "m5",
                chatId = "alice_direct",
                senderName = "Я",
                text = "Да, после 18:30 совершенно свободен",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 12,
                timeFormatted = "12:05",
                isFromMe = true,
                isRead = true,
                type = MessageType.TEXT
            ),
            Message(
                id = "m6",
                chatId = "alice_direct",
                senderName = "Алиса Смирнова",
                text = "Договорились, встретимся в 19:00 в кофейне ☕",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 5,
                timeFormatted = "12:10",
                isFromMe = false,
                isRead = false,
                type = MessageType.TEXT,
                reactions = "❤️"
            ),
            Message(
                id = "m7",
                chatId = "assistant_bot",
                senderName = "TeleGuest Assistant",
                text = "👋 Приветствую в TeleGuest! Я ваш персональный ассистент. Доступны быстрые команды:\n/guest — информация о гостевом режиме\n/joke — случайная шутка\n/crypto — курсы криптовалют",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                timeFormatted = "Вчера",
                isFromMe = false,
                isRead = true,
                type = MessageType.TEXT
            )
        )

        messageDao.insertMessages(initialMessages)
    }
}
