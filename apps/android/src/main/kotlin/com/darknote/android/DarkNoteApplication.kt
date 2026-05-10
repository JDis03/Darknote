package com.darknote.android

import android.app.Application
import com.darknote.sync.client.DropboxClientFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DarkNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DropboxClientFactory.initialize(this)
    }
}
