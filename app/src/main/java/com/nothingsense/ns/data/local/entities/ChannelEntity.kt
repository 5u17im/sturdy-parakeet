package com.nothingsense.ns.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val creatorId: String,
    val isPrivate: Boolean = false,
    val passphraseHash: String? = null,
    val memberCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
