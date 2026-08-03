package com.nothingsense.ns.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.data.update.UpdateInfo
import com.nothingsense.ns.data.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val identityManager: IdentityManager,
    private val updateManager: UpdateManager
) : ViewModel() {

    val userId: StateFlow<String?> = identityManager.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val username: StateFlow<String?> = identityManager.usernameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bio: StateFlow<String> = identityManager.bioFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Explorando la red mesh offline de NoSense")

    val avatarUri: StateFlow<String?> = identityManager.avatarUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val biometricEnabled: StateFlow<Boolean> = identityManager.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoDownload: StateFlow<Boolean> = identityManager.autoDownloadFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<String> = identityManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NoSense Dark")

    private val _updateInfoState = MutableStateFlow<UpdateInfo?>(null)
    val updateInfoState: StateFlow<UpdateInfo?> = _updateInfoState.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            identityManager.setUsername(newUsername)
        }
    }

    fun updateBio(newBio: String) {
        viewModelScope.launch {
            identityManager.setBio(newBio)
        }
    }

    fun updateAvatarUri(newUri: String?) {
        viewModelScope.launch {
            identityManager.setAvatarUri(newUri)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            identityManager.setBiometricEnabled(enabled)
        }
    }

    fun setAutoDownloadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            identityManager.setAutoDownloadEnabled(enabled)
        }
    }

    fun setThemeMode(theme: String) {
        viewModelScope.launch {
            identityManager.setThemeMode(theme)
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = updateManager.checkForUpdates()
            _updateInfoState.value = result
            _isCheckingUpdate.value = false
        }
    }

    fun downloadAndInstallUpdate(updateInfo: UpdateInfo) {
        if (updateInfo.downloadUrl.isNotBlank()) {
            updateManager.startDownloadAndInstall(
                downloadUrl = updateInfo.downloadUrl,
                fileName = "NoSense-v${updateInfo.latestVersionName}.apk"
            )
        }
    }
}
