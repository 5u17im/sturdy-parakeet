package com.nothingsense.ns.data.repository

import android.content.Context
import android.util.Log
import com.nothingsense.ns.network.HybridTransportManager
import com.nothingsense.ns.network.model.MeshPacket
import com.nothingsense.ns.network.model.PacketType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ReputationManager"
private const val PREFS_NAME = "nosense_reputation_prefs"
private const val KEY_BLOCKED_USERS = "blocked_user_ids"
private const val KEY_REPORTS_PREFIX = "reports_count_"

@Singleton
class ReputationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transportManager: HybridTransportManager
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val blockedUsers = ConcurrentHashMap.newKeySet<String>()
    private val reportCounts = ConcurrentHashMap<String, Int>()

    private val _blockedUsersState = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsersState: StateFlow<Set<String>> = _blockedUsersState.asStateFlow()

    init {
        loadFromPrefs()
        observeIncomingReports()
    }

    private fun loadFromPrefs() {
        val savedBlocked = prefs.getStringSet(KEY_BLOCKED_USERS, emptySet()) ?: emptySet()
        blockedUsers.addAll(savedBlocked)
        _blockedUsersState.value = blockedUsers.toSet()

        val allPrefs = prefs.all
        for ((key, value) in allPrefs) {
            if (key.startsWith(KEY_REPORTS_PREFIX) && value is Int) {
                val userId = key.removePrefix(KEY_REPORTS_PREFIX)
                reportCounts[userId] = value
            }
        }
    }

    private fun observeIncomingReports() {
        scope.launch {
            transportManager.incomingPackets.collect { packet ->
                if (packet.type == PacketType.USER_REPORT) {
                    val reportedUserId = packet.content.trim()
                    if (reportedUserId.isNotBlank()) {
                        incrementReportCount(reportedUserId, broadcast = false)
                    }
                }
            }
        }
    }

    fun isUserBlocked(userId: String): Boolean {
        return blockedUsers.contains(userId)
    }

    fun blockUser(userId: String) {
        blockedUsers.add(userId)
        _blockedUsersState.value = blockedUsers.toSet()
        saveBlockedToPrefs()
    }

    fun unblockUser(userId: String) {
        blockedUsers.remove(userId)
        _blockedUsersState.value = blockedUsers.toSet()
        saveBlockedToPrefs()
    }

    private fun saveBlockedToPrefs() {
        prefs.edit().putStringSet(KEY_BLOCKED_USERS, blockedUsers.toSet()).apply()
    }

    fun getReportCount(userId: String): Int {
        return reportCounts[userId] ?: 0
    }

    fun isWarningBadgeRequired(userId: String): Boolean {
        return getReportCount(userId) >= 3
    }

    fun reportUser(reportedUserId: String, reason: String = "spam") {
        incrementReportCount(reportedUserId, broadcast = true)
    }

    private fun incrementReportCount(reportedUserId: String, broadcast: Boolean) {
        val currentCount = reportCounts[reportedUserId] ?: 0
        val newCount = currentCount + 1
        reportCounts[reportedUserId] = newCount
        prefs.edit().putInt("$KEY_REPORTS_PREFIX$reportedUserId", newCount).apply()

        Log.d(TAG, "Incremented report count for $reportedUserId: $newCount (broadcast=$broadcast)")

        if (broadcast) {
            scope.launch {
                val reportPacket = MeshPacket(
                    senderId = "ANONYMOUS_REPORTER",
                    senderName = "Anonymous",
                    type = PacketType.USER_REPORT,
                    content = reportedUserId
                )
                transportManager.sendPacket(reportPacket, null)
            }
        }
    }
}
