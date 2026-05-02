package com.darknote.sync.client

import android.content.Context
import java.io.File

/**
 * Android implementation of DropboxClient.
 * Reuses JVM implementation with Android-specific paths.
 */
actual object DropboxClientFactory {
    private const val APP_KEY = "97rske3f4p28pex"

    actual fun create(): DropboxClient {
        return JvmDropboxClient(APP_KEY)
    }
}