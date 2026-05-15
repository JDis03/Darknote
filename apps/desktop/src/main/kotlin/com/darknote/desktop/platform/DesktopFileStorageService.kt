package com.darknote.desktop.platform

import com.darknote.core.storage.FileStorageService
import java.io.File

object DesktopFileStorageFactory {
    fun create(): FileStorageService {
        val storageDir = getStorageDir()
        return FileStorageService(storageDir)
    }
    
    private fun getStorageDir(): File {
        val userHome = System.getProperty("user.home")
        val appDataDir = when {
            System.getProperty("os.name").lowercase().contains("linux") -> {
                System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
            }
            System.getProperty("os.name").lowercase().contains("mac") -> {
                "$userHome/Library/Application Support"
            }
            else -> { // Windows
                System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
            }
        }
        
        return File("$appDataDir/DarkNote/snippets").apply {
            mkdirs()
        }
    }
}
