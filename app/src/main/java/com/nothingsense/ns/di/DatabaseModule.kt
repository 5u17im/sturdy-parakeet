package com.nothingsense.ns.di

import android.content.Context
import androidx.room.Room
import com.nothingsense.ns.data.local.AppDatabase
import com.nothingsense.ns.data.local.dao.ChatDao
import com.nothingsense.ns.data.local.dao.MessageDao
import com.nothingsense.ns.data.local.dao.StatusDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nosense_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideStatusDao(database: AppDatabase): StatusDao = database.statusDao()
}
