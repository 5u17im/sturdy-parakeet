package com.nothingsense.ns.di

import android.content.Context
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.network.MeshManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMeshManager(
        @ApplicationContext context: Context,
        identityManager: IdentityManager
    ): MeshManager {
        return MeshManager(context, identityManager)
    }
}
