package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chat
import com.example.ui.ChatFilterTab
import com.example.ui.components.ChatListItem
import com.example.ui.components.GuestModeBanner
import com.example.ui.components.TelegramTopBar
import com.example.ui.theme.GuestOrange

@Composable
fun ChatListScreen(
    chats: List<Chat>,
    selectedTab: ChatFilterTab,
    searchQuery: String,
    isGuestMode: Boolean,
    onTabSelected: (ChatFilterTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onChatClick: (Chat) -> Unit,
    onMenuClick: () -> Unit,
    onGuestModeToggleClick: () -> Unit,
    onExitGuestModeClick: () -> Unit,
    onOpenGuestSettingsClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onTogglePin: (Chat) -> Unit,
    onToggleMute: (Chat) -> Unit,
    onDeleteChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TelegramTopBar(
                isGuestMode = isGuestMode,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onMenuClick = onMenuClick,
                onGuestModeToggleClick = onGuestModeToggleClick
            )

            // Guest Banner (if active)
            GuestModeBanner(
                isGuestActive = isGuestMode,
                onExitClick = onExitGuestModeClick,
                onSettingsClick = onOpenGuestSettingsClick
            )

            // Filter Tabs (Все, Личные, Группы, Каналы, Боты)
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = if (isGuestMode) GuestOrange else MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                },
                divider = {}
            ) {
                ChatFilterTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = if (isSelected) {
                                    if (isGuestMode) GuestOrange else MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Chat Items or Empty state
            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isGuestMode) Icons.Default.Lock else Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isGuestMode) "Нет доступных чатов в гостевом режиме" else "Чаты не найдены",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isGuestMode)
                                "Личные чаты скрыты правилами безопасности гостевого режима."
                            else
                                "Попробуйте изменить запрос поиска или начните новый диалог.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(chats, key = { it.id }) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onChatClick(chat) },
                            onTogglePin = { onTogglePin(chat) },
                            onToggleMute = { onToggleMute(chat) },
                            onDelete = { onDeleteChat(chat.id) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            modifier = Modifier.padding(start = 78.dp)
                        )
                    }
                }
            }
        }

        // New Chat Floating Action Button
        FloatingActionButton(
            onClick = onNewChatClick,
            containerColor = if (isGuestMode) GuestOrange else MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Написать сообщение",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
