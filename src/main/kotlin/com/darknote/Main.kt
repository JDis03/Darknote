package com.darknote

import com.darknote.clipboard.ClipboardManager
import com.darknote.sync.DropboxClient
import com.darknote.sync.FileWatcher
import kotlinx.coroutines.*
import java.io.File
import java.util.Scanner

fun main() = runBlocking {
    println("--- Darknote: Iniciando ---")

    // TODO: Reemplazar con una App Key real o cargar desde configuración
    val appKey = System.getenv("DROPBOX_APP_KEY") ?: "TU_APP_KEY_AQUI"
    val dropboxClient = DropboxClient(appKey)

    if (!dropboxClient.isAuthorized()) {
        println("Dropbox no está autorizado.")
        println("Por favor, visita este enlace para autorizar la aplicación:")
        println(dropboxClient.getAuthUrl())
        
        print("\nIntroduce el código de autorización: ")
        val scanner = Scanner(System.`in`)
        val code = scanner.nextLine()
        
        try {
            dropboxClient.finishAuth(code)
        } catch (e: Exception) {
            println("Error al autorizar: ${e.message}")
            return@runBlocking
        }
    }

    val notesDir = File(System.getProperty("user.home"), ".config/knotes/notes/")
    
    // Lanzar monitor de archivos en segundo plano
    val watcherJob = launch(Dispatchers.IO) {
        val watcher = FileWatcher(notesDir) { file ->
            println("Archivo modificado: ${file.name}. Preparando sincronización...")
            // TODO: Implementar lógica de subida a Dropbox aquí
        }
        watcher.startWatching()
    }

    // Lanzar limpiador de portapapeles en segundo plano (polling cada 2 segundos)
    val clipboardJob = launch(Dispatchers.IO) {
        while (isActive) {
            ClipboardManager.sanitize()
            delay(2000)
        }
    }

    println("Darknote en ejecución. Presiona Ctrl+C para salir.")
    
    // Mantener la aplicación viva
    try {
        watcherJob.join()
        clipboardJob.join()
    } catch (e: CancellationException) {
        println("Apagando Darknote...")
    }
}
