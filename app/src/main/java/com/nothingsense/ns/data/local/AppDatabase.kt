package com.nothingsense.ns.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nothingsense.ns.data.local.dao.ChatDao
import com.nothingsense.ns.data.local.dao.MessageDao
import com.nothingsense.ns.data.local.dao.StatusDao
import com.nothingsense.ns.data.local.entities.ChatEntity
import com.nothingsense.ns.data.local.entities.MessageEntity
import com.nothingsense.ns.data.local.entities.StatusEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class, StatusEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun statusDao(): StatusDao
}
