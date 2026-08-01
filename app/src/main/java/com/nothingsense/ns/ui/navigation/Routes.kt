package com.nothingsense.ns.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data object ChatList : Route

    @Serializable
    data class ChatDetail(
        val chatId: String,
        val chatName: String
    ) : Route

    @Serializable
    data object StatusList : Route

    @Serializable
    data object Channels : Route

    @Serializable
    data object Settings : Route
}
