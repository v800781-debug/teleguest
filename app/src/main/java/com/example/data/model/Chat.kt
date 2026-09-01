package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChatType {
    DIRECT,
    GROUP,
    CHANNEL,
    BOT,
    SAVED_MESSAGES
}

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String,
    val title: String,
    val username: String? = null,
    val type: ChatType = ChatType.DIRECT,
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val avatarColorHex: String = "#2AABEE",
    val memberCount: Int = 0,
    val bio: String = "",
    val isPrivateToOwner: Boolean = false, // If true, HIDDEN when Guest Mode is active
    val isCreatedByGuest: Boolean = false  // If true, cleared when guest mode exits (if configured)
)
