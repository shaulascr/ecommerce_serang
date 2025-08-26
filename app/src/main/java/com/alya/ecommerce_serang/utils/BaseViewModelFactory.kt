package com.alya.ecommerce_serang.utils

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.UserRepository
import com.alya.ecommerce_serang.utils.viewmodel.RegisterStoreViewModel

class BaseViewModelFactory<VM : ViewModel>(
    private val creator: () -> VM
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }
}

// Add a new factory for SavedStateHandle ViewModels
class SavedStateViewModelFactory<VM : ViewModel>(
    private val owner: SavedStateRegistryOwner,
    private val creator: (SavedStateHandle) -> VM
) : AbstractSavedStateViewModelFactory(owner, null) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return creator(handle) as T
    }
}

class RegisterStoreViewModelFactory(
    private val owner: SavedStateRegistryOwner,
    private val defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return when {
            modelClass.isAssignableFrom(RegisterStoreViewModel::class.java) -> {
                // Create SessionManager and ApiService
                val context = if (owner is Context) owner else (owner as Fragment).requireContext()
                val sessionManager = SessionManager(context)
                val apiService = ApiConfig.getApiService(sessionManager)
                val repository = UserRepository(apiService)

                RegisterStoreViewModel(repository, handle) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}