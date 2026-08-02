package com.nothingsense.ns.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingsense.ns.data.identity.IdentityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val identityManager: IdentityManager
) : ViewModel() {

    val userId: StateFlow<String?> = identityManager.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val username: StateFlow<String?> = identityManager.usernameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bio: StateFlow<String> = identityManager.bioFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Explorando la red mesh offline de NoSense")

    val avatarUri: StateFlow<String?> = identityManager.avatarUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
}
