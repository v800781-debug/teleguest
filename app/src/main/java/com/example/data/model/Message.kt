package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType {
    TEXT,
    PHOTO,
    VOICE,
    STICKER,
    SYSTEM
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = "12:00",
    val isFromMe: Boolean = false,
    val isRead: Boolean = true,
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val voiceDurationSeconds: Int = 0,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: String = "", // Comma-separated emoji reactions e.g. "👍,❤️"
    val viewsCount: String? = null,
    val isGuestMessage: Boolean = false // If true, created during guest mode
)
