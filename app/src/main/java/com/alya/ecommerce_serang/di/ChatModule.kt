package com.alya.ecommerce_serang.di

import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.ChatRepository
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.ui.chat.SocketIOService
import com.alya.ecommerce_serang.utils.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatRepository(apiService: ApiService): ChatRepository {
        return ChatRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideUserRepository(apiService: ApiService): UserRepository {
        return UserRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideSocketIOService(sessionManager: SessionManager): SocketIOService {
        return SocketIOService(sessionManager)
    }
}