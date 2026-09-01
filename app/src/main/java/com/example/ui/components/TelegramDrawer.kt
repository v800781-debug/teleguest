package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.GuestAmber
import com.example.ui.theme.GuestBadgeColor
import com.example.ui.theme.GuestOrange
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramHeaderBlue

@Composable
fun TelegramDrawer(
    userProfile: UserProfile,
    isGuestMode: Boolean,
    onEnterGuestMode: () -> Unit,
    onExitGuestMode: () -> Unit,
    onOpenGuestSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onNewGroup: () -> Unit,
    onNewChannel: () -> Unit,
    onOpenSavedMessages: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initials = rememberUserInitials(userProfile.name)

    Surface(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Profile Banner
            val headerBrush = if (isGuestMode) {
                Brush.verticalGradient(listOf(GuestOrange, Color(0xFFE65100)))
            } else {
                Brush.verticalGradient(listOf(TelegramHeaderBlue, TelegramBlue))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBrush)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGuestMode) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Text(
                                    text = initials,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Badge
                        if (isGuestMode) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ГОСТЬ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GuestBadgeColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isGuestMode) "Гостевой профиль" else userProfile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    Text(
                        text = if (isGuestMode) "Режим приватности активен" else "${userProfile.phone} • ${userProfile.username}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Guest Mode Highlight Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable {
                        if (isGuestMode) onExitGuestMode() else onEnterGuestMode()
                    }
                    .testTag("drawer_guest_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGuestMode) GuestAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isGuestMode) GuestOrange else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGuestMode) Icons.Default.LockOpen else Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGuestMode) "Выйти из гостя" else "Гостевой режим",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isGuestMode) "Разблокировать по PIN-коду" else "Скрыть личные чаты",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Menu Items
            DrawerMenuItem(
                icon = Icons.Default.Settings,
                title = "Настройки гостевого режима",
                subtitle = "PIN, автоочистка, фильтры",
                tag = "drawer_guest_settings",
                onClick = {
                    onCloseDrawer()
                    onOpenGuestSettings()
                }
            )

            if (!isGuestMode) {
                DrawerMenuItem(
                    icon = Icons.Default.Edit,
                    title = "Редактировать профиль",
                    subtitle = "Изменить имя, юзернейм, о себе",
                    tag = "drawer_edit_profile",
                    onClick = {
                        onCloseDrawer()
                        onEditProfile()
                    }
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 16.dp)
            )

            if (!isGuestMode) {
                DrawerMenuItem(
                    icon = Icons.Default.Bookmark,
                    title = "Избранное",
                    subtitle = "Личные заметки и файлы",
                    tag = "drawer_saved_messages",
                    onClick = {
                        onCloseDrawer()
                        onOpenSavedMessages()
                    }
                )
            }

            DrawerMenuItem(
                icon = Icons.Default.Group,
                title = "Создать группу",
                tag = "drawer_new_group",
                onClick = {
                    onCloseDrawer()
                    onNewGroup()
                }
            )

            DrawerMenuItem(
                icon = Icons.Outlined.Campaign,
                title = "Создать канал",
                tag = "drawer_new_channel",
                onClick = {
                    onCloseDrawer()
                    onNewChannel()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Person,
                title = "Контакты",
                tag = "drawer_contacts",
                onClick = { onCloseDrawer() }
            )

            DrawerMenuItem(
                icon = Icons.Default.Call,
                title = "Звонки",
                tag = "drawer_calls",
                onClick = { onCloseDrawer() }
            )

            DrawerMenuItem(
                icon = Icons.Default.Star,
                title = "TeleGuest Premium",
                tag = "drawer_premium",
                onClick = { onCloseDrawer() }
            )

            DrawerMenuItem(
                icon = Icons.Default.ExitToApp,
                title = "Сменить аккаунт / Выйти",
                tag = "drawer_logout",
                onClick = {
                    onCloseDrawer()
                    onLogout()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TeleGuest v1.0 • Guest Shield Active",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun rememberUserInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "ТГ"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

