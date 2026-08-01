package com.nothingsense.ns.network.model

data class MeshNode(
    val endpointId: String,
    val userId: String,
    val username: String,
    val isConnected: Boolean = false
)
