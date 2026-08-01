package com.nothingsense.ns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nothingsense.ns.data.local.entities.StatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses WHERE expiresAt > :currentTime ORDER BY timestamp DESC")
    fun getActiveStatuses(currentTime: Long): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    @Query("DELETE FROM statuses WHERE expiresAt <= :currentTime")
    suspend fun deleteExpiredStatuses(currentTime: Long)
}
