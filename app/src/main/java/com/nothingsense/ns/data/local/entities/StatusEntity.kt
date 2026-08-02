package com.nothingsense.ns.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val content: String,
    val timestamp: Long,
    val expiresAt: Long,
    val imageUri: String? = null
)
