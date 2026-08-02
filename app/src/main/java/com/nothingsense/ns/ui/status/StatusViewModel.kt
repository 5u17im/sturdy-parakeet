package com.nothingsense.ns.ui.status

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.data.local.entities.StatusEntity
import com.nothingsense.ns.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val repository: StatusRepository,
    private val identityManager: IdentityManager
) : ViewModel() {

    val activeStatuses: StateFlow<List<StatusEntity>> = repository.getActiveStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun postStatus(content: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            repository.postStatus(content, imageUri)
        }
    }
}
