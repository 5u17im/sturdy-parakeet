package com.nothingsense.ns.di

import android.content.Context
import com.nothingsense.ns.data.identity.IdentityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IdentityModule {

    @Provides
    @Singleton
    fun provideIdentityManager(@ApplicationContext context: Context): IdentityManager {
        return IdentityManager(context)
    }
}
