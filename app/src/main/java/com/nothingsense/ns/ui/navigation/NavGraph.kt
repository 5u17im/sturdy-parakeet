package com.nothingsense.ns.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nothingsense.ns.R
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nothingsense.ns.ui.chat.ChatDetailScreen
import com.nothingsense.ns.ui.chat.ChatListScreen
import com.nothingsense.ns.ui.chat.ChatViewModel
import com.nothingsense.ns.ui.status.StatusScreen
import com.nothingsense.ns.ui.status.StatusViewModel
import com.nothingsense.ns.ui.channel.ChannelScreen
import com.nothingsense.ns.ui.onboarding.OnboardingScreen

import androidx.compose.material.icons.rounded.Settings
import com.nothingsense.ns.ui.settings.SettingsScreen
import com.nothingsense.ns.ui.settings.SettingsViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val statusViewModel: StatusViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    
    val onboardingCompleted by chatViewModel.onboardingCompleted.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(Unit) {
        chatViewModel.incomingCallEvents.collect { event ->
            if (event.signal == "OFFER") {
                navController.navigate(Route.Call(event.senderId, event.senderName, isIncoming = true))
            }
        }
    }

    if (onboardingCompleted == null) return

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination?.hasRoute<Route.ChatDetail>() == false && 
                currentDestination?.hasRoute<Route.Onboarding>() == false &&
                currentDestination?.hasRoute<Route.Profile>() == false &&
                currentDestination?.hasRoute<Route.Call>() == false &&
                currentDestination?.hasRoute<Route.PeerProfile>() == false) {
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
                        items.forEach { (label, route, icon) ->
                            val isSelected = currentDestination.hierarchy.any { it.hasRoute(route::class) }
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
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
                ChatListScreen(
                    viewModel = chatViewModel,
                    onChatClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    }
                )
            }
            composable<Route.StatusList> {
                StatusScreen(viewModel = statusViewModel)
            }
            composable<Route.Channels> {
                ChannelScreen(
                    viewModel = chatViewModel,
                    onChannelClick = { chat ->
                        navController.navigate(Route.ChatDetail(chat.id, chat.name))
                    }
                )
            }
            composable<Route.Settings> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.Profile> {
                val username by settingsViewModel.username.collectAsState()
                val bio by settingsViewModel.bio.collectAsState()
                val avatarUri by settingsViewModel.avatarUri.collectAsState()
                val userId by settingsViewModel.userId.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                val cryptoManager = remember { com.nothingsense.ns.security.CryptoManager(context) }

                com.nothingsense.ns.ui.profile.ProfileScreen(
                    currentUsername = username ?: "Usuario",
                    currentBio = bio,
                    currentAvatarUri = avatarUri,
                    userId = userId ?: "unknown",
                    cryptoManager = cryptoManager,
                    onSaveProfile = { newUsername, newBio, newAvatarUri ->
                        settingsViewModel.updateUsername(newUsername)
                        settingsViewModel.updateBio(newBio)
                        settingsViewModel.updateAvatarUri(newAvatarUri)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.ChatDetail> { backStackEntry ->
                val chatDetail: Route.ChatDetail = backStackEntry.toRoute()
                val context = androidx.compose.ui.platform.LocalContext.current
                val reputationManager = remember { com.nothingsense.ns.data.repository.ReputationManager(context, chatViewModel.transportManager) }

                ChatDetailScreen(
                    chatId = chatDetail.chatId,
                    chatName = chatDetail.chatName,
                    viewModel = chatViewModel,
                    onNavigateToPeerProfile = { targetUserId, targetName ->
                        navController.navigate(Route.PeerProfile(targetUserId, targetName))
                    },
                    onStartCall = { targetUserId, targetName ->
                        navController.navigate(Route.Call(targetUserId, targetName, false))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.PeerProfile> { backStackEntry ->
                val peerProfile: Route.PeerProfile = backStackEntry.toRoute()
                val context = androidx.compose.ui.platform.LocalContext.current
                val reputationManager = remember { com.nothingsense.ns.data.repository.ReputationManager(context, chatViewModel.transportManager) }

                com.nothingsense.ns.ui.profile.PeerProfileScreen(
                    userId = peerProfile.userId,
                    username = peerProfile.username,
                    reputationManager = reputationManager,
                    onStartChat = {
                        navController.navigate(Route.ChatDetail(peerProfile.userId, peerProfile.username))
                    },
                    onStartCall = {
                        navController.navigate(Route.Call(peerProfile.userId, peerProfile.username, false))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<Route.Call> { backStackEntry ->
                val callRoute: Route.Call = backStackEntry.toRoute()
                com.nothingsense.ns.ui.call.CallScreen(
                    peerUsername = callRoute.username,
                    peerUserId = callRoute.userId,
                    isIncoming = callRoute.isIncoming,
                    viewModel = chatViewModel,
                    onEndCall = { navController.popBackStack() }
                )
            }
        }
    }
}
