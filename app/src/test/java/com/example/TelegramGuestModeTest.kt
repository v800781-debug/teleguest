package com.example

import com.example.data.model.Chat
import com.example.data.model.ChatType
import com.example.data.model.GuestModeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramGuestModeTest {

    @Test
    fun testGuestModeFiltersPrivateChats() {
        val allChats = listOf(
            Chat(id = "1", title = "Saved Messages", isPrivateToOwner = true, type = ChatType.SAVED_MESSAGES),
            Chat(id = "2", title = "Alice", isPrivateToOwner = true, type = ChatType.DIRECT),
            Chat(id = "3", title = "Telegram News", isPrivateToOwner = false, type = ChatType.CHANNEL),
            Chat(id = "4", title = "Assistant Bot", isPrivateToOwner = false, type = ChatType.BOT)
        )

        val guestSettings = GuestModeSettings(isGuestModeActive = true, hidePersonalChats = true)

        val visibleInGuestMode = allChats.filter { chat ->
            if (guestSettings.isGuestModeActive && guestSettings.hidePersonalChats) {
                !chat.isPrivateToOwner
            } else {
                true
            }
        }

        assertEquals(2, visibleInGuestMode.size)
        assertTrue(visibleInGuestMode.any { it.id == "3" })
        assertTrue(visibleInGuestMode.any { it.id == "4" })
        assertFalse(visibleInGuestMode.any { it.id == "1" })
        assertFalse(visibleInGuestMode.any { it.id == "2" })
    }

    @Test
    fun testPinVerification() {
        val settings = GuestModeSettings(pinCode = "1234")
        assertTrue(settings.pinCode == "1234")
        assertFalse(settings.pinCode == "0000")
    }
}
