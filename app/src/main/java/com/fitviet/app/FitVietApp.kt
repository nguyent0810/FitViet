package com.fitviet.app

import android.app.Application
import com.fitviet.app.data.AppContainer

class FitVietApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
