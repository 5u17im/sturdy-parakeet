package com.nothingsense.ns.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nothingsense.ns.R
import com.nothingsense.ns.data.local.entities.ChatEntity
import com.nothingsense.ns.data.repository.ReputationManager
import com.nothingsense.ns.security.CryptoManager
import com.nothingsense.ns.ui.call.CallScreen
import com.nothingsense.ns.ui.channel.ChannelScreen
import com.nothingsense.ns.ui.chat.ChatDetailScreen
import com.nothingsense.ns.ui.chat.ChatListScreen
import com.nothingsense.ns.ui.chat.ChatViewModel
import com.nothingsense.ns.ui.onboarding.OnboardingScreen
import com.nothingsense.ns.ui.profile.PeerProfileScreen
import com.nothingsense.ns.ui.profile.ProfileScreen
import com.nothingsense.ns.ui.settings.SettingsScreen
import com.nothingsense.ns.ui.settings.SettingsViewModel
import com.nothingsense.ns.ui.status.StatusScreen
import com.nothingsense.ns.ui.status.StatusViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val statusViewModel: StatusViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val onboardingCompleted by chatViewModel.onboardingCompleted.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })

    LaunchedEffect(Unit) {
        chatViewModel.incomingCallEvents.collect { event ->
            if (event.signal == "OFFER") {
                navController.navigate(Route.Call(event.senderId, event.senderName, isIncoming = true))
            }
        }
    }

    if (onboardingCompleted == null) return

    val isMainTabScreen = currentDestination?.hasRoute<Route.ChatList>() == true ||
            currentDestination?.hasRoute<Route.StatusList>() == true ||
            currentDestination?.hasRoute<Route.Channels>() == true ||
            currentDestination?.hasRoute<Route.Settings>() == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isMainTabScreen) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.85f),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        val items = listOf(
                            Triple(stringResource(R.string.chats), Route.ChatList, Icons.AutoMirrored.Rounded.Chat),
                            Triple(stringResource(R.string.status), Route.StatusList, Icons.Rounded.History),
                            Triple(stringResource(R.string.channels), Route.Channels, Icons.Rounded.Campaign),
                            Triple(stringResource(R.string.settings), Route.Settings, Icons.Rounded.Settings)
                        )
                        items.forEachIndexed { index, (label, route, icon) ->
                            val isSelected = pagerState.currentPage == index
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                selected = isSelected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted == true) Route.ChatList else Route.Onboarding,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Route.Onboarding> {
                OnboardingScreen(onComplete = {
                    navController.navigate(Route.ChatList) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                })
            }
            composable<Route.ChatList> {
                MainHorizontalPagerContainer(
                    pagerState = pagerState,
                    chatViewModel = chatViewModel,
                    statusViewModel = statusViewModel,
                    settingsViewModel = settingsViewModel,
                    onChatClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    },
                    onChannelClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.StatusList> {
                MainHorizontalPagerContainer(
                    pagerState = pagerState,
                    chatViewModel = chatViewModel,
                    statusViewModel = statusViewModel,
                    settingsViewModel = settingsViewModel,
                    onChatClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    },
                    onChannelClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.Channels> {
                MainHorizontalPagerContainer(
                    pagerState = pagerState,
                    chatViewModel = chatViewModel,
                    statusViewModel = statusViewModel,
                    settingsViewModel = settingsViewModel,
                    onChatClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    },
                    onChannelClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.Settings> {
                MainHorizontalPagerContainer(
                    pagerState = pagerState,
                    chatViewModel = chatViewModel,
                    statusViewModel = statusViewModel,
                    settingsViewModel = settingsViewModel,
                    onChatClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    },
                    onChannelClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.Profile> {
                val username by settingsViewModel.username.collectAsState()
                val bio by settingsViewModel.bio.collectAsState()
                val avatarUri by settingsViewModel.avatarUri.collectAsState()
                val userId by settingsViewModel.userId.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                val cryptoManager = remember { CryptoManager(context) }

                ProfileScreen(
                    currentUsername = username ?: "Usuario",
                    currentBio = bio,
                    currentAvatarUri = avatarUri,
                    userId = userId ?: "...",
                    cryptoManager = cryptoManager,
                    onSaveProfile = { newName, newBio, newAvatar ->
                        settingsViewModel.updateUsername(newName)
                        settingsViewModel.updateBio(newBio)
                        if (newAvatar != null) {
                            settingsViewModel.updateAvatarUri(newAvatar)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.PeerProfile> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.PeerProfile>()
                val context = androidx.compose.ui.platform.LocalContext.current
                val reputationManager = remember { ReputationManager(context, chatViewModel.transportManager) }

                PeerProfileScreen(
                    userId = args.userId,
                    username = args.username,
                    reputationManager = reputationManager,
                    transportManager = chatViewModel.transportManager,
                    onStartChat = {
                        navController.navigate(Route.ChatDetail(args.userId, args.username))
                    },
                    onStartCall = {
                        navController.navigate(Route.Call(args.userId, args.username, isIncoming = false))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.ChatDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.ChatDetail>()
                ChatDetailScreen(
                    chatId = args.chatId,
                    chatName = args.chatName,
                    viewModel = chatViewModel,
                    onNavigateToPeerProfile = { peerId, peerName ->
                        navController.navigate(Route.PeerProfile(peerId, peerName))
                    },
                    onStartCall = { peerId, peerName ->
                        navController.navigate(Route.Call(peerId, peerName, isIncoming = false))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.Call> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.Call>()
                CallScreen(
                    peerUserId = args.userId,
                    peerUsername = args.username,
                    isIncoming = args.isIncoming,
                    viewModel = chatViewModel,
                    onEndCall = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MainHorizontalPagerContainer(
    pagerState: androidx.compose.foundation.pager.PagerState,
    chatViewModel: ChatViewModel,
    statusViewModel: StatusViewModel,
    settingsViewModel: SettingsViewModel,
    onChatClick: (ChatEntity) -> Unit,
    onChannelClick: (ChatEntity) -> Unit,
    onBack: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> ChatListScreen(viewModel = chatViewModel, onChatClick = onChatClick)
            1 -> StatusScreen(viewModel = statusViewModel)
            2 -> ChannelScreen(viewModel = chatViewModel, onChannelClick = onChannelClick)
            3 -> SettingsScreen(viewModel = settingsViewModel, onBack = onBack)
        }
    }
}
