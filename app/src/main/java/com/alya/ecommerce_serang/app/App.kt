package com.alya.ecommerce_serang.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application(){
//    override fun onCreate() {
//        super.onCreate()
//
//        val sessionManager = SessionManager(this)
//        if (sessionManager.getUserId() != null) {
//            val serviceIntent = Intent(this, SimpleWebSocketService::class.java)
//            startService(serviceIntent)
//        }
//    }

}