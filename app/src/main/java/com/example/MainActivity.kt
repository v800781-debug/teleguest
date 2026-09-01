package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.TelegramViewModel
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.GuestLockDialog
import com.example.ui.components.GuestSettingsDialog
import com.example.ui.components.NewChatDialog
import com.example.ui.components.TelegramDrawer
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: TelegramViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TelegramApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TelegramApp(viewModel: TelegramViewModel) {
    val filteredChats by viewModel.filteredChats.collectAsStateWithLifecycle()
    val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val guestSettings by viewModel.guestSettings.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val replyingToMessage by viewModel.replyingToMessage.collectAsStateWithLifecycle()

    val isPinUnlockOpen by viewModel.isPinUnlockDialogOpen.collectAsStateWithLifecycle()
    val isGuestSettingsOpen by viewModel.isGuestSettingsDialogOpen.collectAsStateWithLifecycle()
    val isNewChatOpen by viewModel.isNewChatDialogOpen.collectAsStateWithLifecycle()
    val notification by viewModel.statusNotification.collectAsStateWithLifecycle()

    var isEditProfileOpen by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notification) {
        notification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // If not logged in, show the Login / Onboarding screen
    if (!userProfile.isLoggedIn) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            LoginScreen(
                onLoginWithPhone = { phone, name, username ->
                    viewModel.loginWithPhone(phone, name, username)
                },
                onLoginAsGuest = { nickname ->
                    viewModel.loginAsGuest(nickname)
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    // Intercept back button if in Chat Details or Drawer is open
    BackHandler(enabled = activeChat != null || drawerState.isOpen) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (activeChat != null) {
            viewModel.closeChat()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = activeChat == null,
        drawerContent = {
            TelegramDrawer(
                userProfile = userProfile,
                isGuestMode = guestSettings.isGuestModeActive,
                onEnterGuestMode = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.enterGuestMode()
                },
                onExitGuestMode = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.requestExitGuestMode()
                },
                onOpenGuestSettings = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openGuestSettingsDialog(true)
                },
                onEditProfile = {
                    isEditProfileOpen = true
                },
                onLogout = {
                    viewModel.logout()
                },
                onNewGroup = { viewModel.openNewChatDialog(true) },
                onNewChannel = { viewModel.openNewChatDialog(true) },
                onOpenSavedMessages = {
                    viewModel.openChat("saved_messages")
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = activeChat,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ChatScreenTransition"
                ) { currentChat ->
                    if (currentChat != null) {
                        ChatDetailScreen(
                            chat = currentChat,
                            messages = activeMessages,
                            isGuestMode = guestSettings.isGuestModeActive,
                            allowSending = guestSettings.allowSendingMessages,
                            replyingToMessage = replyingToMessage,
                            onBackClick = { viewModel.closeChat() },
                            onSendMessage = { text, type, mediaUrl, duration ->
                                viewModel.sendMessage(text, type, mediaUrl, duration)
                            },
                            onSendVoice = { duration ->
                                viewModel.sendVoiceMessage(duration)
                            },
                            onSendSticker = { sticker ->
                                viewModel.sendSticker(sticker)
                            },
                            onToggleReaction = { message, emoji ->
                                viewModel.toggleReaction(message, emoji)
                            },
                            onReplyToMessage = { message ->
                                viewModel.setReplyingTo(message)
                            },
                            onClearHistory = {
                                viewModel.deleteChat(currentChat.id)
                            },
                            onDeleteChat = {
                                viewModel.deleteChat(currentChat.id)
                            }
                        )
                    } else {
                        ChatListScreen(
                            chats = filteredChats,
                            selectedTab = selectedTab,
                            searchQuery = searchQuery,
                            isGuestMode = guestSettings.isGuestModeActive,
                            onTabSelected = { tab -> viewModel.selectTab(tab) },
                            onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                            onChatClick = { chat -> viewModel.openChat(chat.id) },
                            onMenuClick = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onGuestModeToggleClick = {
                                if (guestSettings.isGuestModeActive) {
                                    viewModel.requestExitGuestMode()
                                } else {
                                    viewModel.enterGuestMode()
                                }
                            },
                            onExitGuestModeClick = {
                                viewModel.requestExitGuestMode()
                            },
                            onOpenGuestSettingsClick = {
                                viewModel.openGuestSettingsDialog(true)
                            },
                            onNewChatClick = {
                                viewModel.openNewChatDialog(true)
                            },
                            onTogglePin = { chat -> viewModel.togglePinChat(chat) },
                            onToggleMute = { chat -> viewModel.toggleMuteChat(chat) },
                            onDeleteChat = { id -> viewModel.deleteChat(id) }
                        )
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    EditProfileDialog(
        isOpen = isEditProfileOpen,
        userProfile = userProfile,
        onDismiss = { isEditProfileOpen = false },
        onSaveProfile = { name, username, bio ->
            viewModel.updateUserProfile(name, username, bio)
            isEditProfileOpen = false
        }
    )

    // PIN Unlock Dialog
    GuestLockDialog(
        isOpen = isPinUnlockOpen,
        onDismiss = { viewModel.openPinUnlockDialog(false) },
        onVerifyPin = { pin -> viewModel.verifyAndExitGuestMode(pin) }
    )

    // Guest Mode Settings Dialog
    GuestSettingsDialog(
        isOpen = isGuestSettingsOpen,
        settings = guestSettings,
        onDismiss = { viewModel.openGuestSettingsDialog(false) },
        onSaveSettings = { newSettings -> viewModel.updateGuestSettings(newSettings) },
        onClearGuestDataNow = { viewModel.clearGuestDataNow() }
    )

    // New Chat / Channel Dialog
    NewChatDialog(
        isOpen = isNewChatOpen,
        isGuestMode = guestSettings.isGuestModeActive,
        onDismiss = { viewModel.openNewChatDialog(false) },
        onCreateChat = { title, username, type, isPrivate, bio ->
            viewModel.createNewChat(title, username, type, isPrivate, bio)
        }
    )
}
