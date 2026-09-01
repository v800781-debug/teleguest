package com.example.data.model

data class GuestModeSettings(
    val isGuestModeActive: Boolean = false,
    val hidePersonalChats: Boolean = true,
    val allowSendingMessages: Boolean = true,
    val clearHistoryOnExit: Boolean = true,
    val pinCode: String = "1234",
    val guestNickname: String = "Гость",
    val sessionStartTime: Long = 0L
)

data class UserProfile(
    val name: String = "Пользователь",
    val username: String = "@user",
    val phone: String = "+7 (999) 000-00-00",
    val bio: String = "Пользуюсь TeleGuest ✨",
    val isPremium: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isGuestAccount: Boolean = false
)

