package com.darknote.sync

import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileWatcher(private val notesDir: File, private val onChange: (File) -> Unit) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()

    init {
        if (!notesDir.exists()) {
            notesDir.mkdirs()
        }
        notesDir.toPath().register(
            watchService,
            ENTRY_CREATE,
            ENTRY_DELETE,
            ENTRY_MODIFY
        )
    }

    /**
     * Bucle de escucha infinito que detecta cambios en los archivos.
     * Debe ejecutarse en un hilo separado o corrutina.
     */
    suspend fun startWatching() = withContext(Dispatchers.IO) {
        println("Monitoreando cambios en: ${notesDir.absolutePath}")
        while (true) {
            val key: WatchKey = watchService.take() ?: break
            for (event in key.pollEvents()) {
                val kind = event.kind()
                if (kind == OVERFLOW) continue

                val fileName = event.context() as Path
                val file = File(notesDir, fileName.toString())
                
                // Solo nos interesan archivos .md
                if (file.name.endsWith(".md")) {
                    println("Evento detectado: $kind en ${file.name}")
                    onChange(file)
                }
            }
            if (!key.reset()) break
        }
    }
}
