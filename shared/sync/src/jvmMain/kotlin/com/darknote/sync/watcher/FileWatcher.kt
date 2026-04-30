package com.darknote.sync.watcher

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * File system watcher that monitors a directory for changes and emits events.
 *
 * Uses Java NIO WatchService for efficient file system monitoring.
 * Includes debounce to avoid triggering rapid successive syncs when
 * a file is being written incrementally.
 *
 * @param directory The directory to watch (defaults to ~/.config/darknote/snippets/)
 * @param scope CoroutineScope for the watching coroutine
 * @param debounceMs Milliseconds to wait after last change before emitting (default 1500ms)
 * @param fileExtensions Only watch files with these extensions (empty = watch all)
 */
class FileWatcher(
    private val directory: File = File(System.getProperty("user.home"), ".config/darknote/snippets"),
    private val scope: CoroutineScope,
    private val debounceMs: Long = 1500L,
    private val fileExtensions: Set<String> = setOf("txt", "md", "json")
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val isRunning = AtomicBoolean(false)
    private val watchedDirectories = ConcurrentHashMap<Path, WatchKey>()
    private var watchJob: Job? = null

    // Inner event stream
    private val _events = MutableSharedFlow<FileChangeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<FileChangeEvent> = _events.asSharedFlow()

    // Debounced change notification - only emits after quiet period
    private val _debouncedChanges = MutableStateFlow(false)
    val debouncedChanges: StateFlow<Boolean> = _debouncedChanges.asStateFlow()

    private var debounceJob: Job? = null

    init {
        ensureDirectoryExists()
        registerDirectory(directory.toPath())
    }

    /**
     * Ensure the watched directory exists.
     */
    private fun ensureDirectoryExists() {
        if (!directory.exists()) {
            directory.mkdirs()
            println("[FileWatcher] Created directory: ${directory.absolutePath}")
        }
    }

    /**
     * Register a directory path with the WatchService.
     */
    private fun registerDirectory(path: Path) {
        try {
            val key = path.register(
                watchService,
                ENTRY_CREATE,
                ENTRY_DELETE,
                ENTRY_MODIFY
            )
            watchedDirectories[path] = key
            println("[FileWatcher] Registered directory: $path")
        } catch (e: Exception) {
            println("[FileWatcher] Failed to register directory $path: ${e.message}")
        }
    }

    /**
     * Start watching for file changes in the background.
     * Runs on Dispatchers.IO to avoid blocking the main thread.
     */
    fun start() {
        if (isRunning.getAndSet(true)) {
            println("[FileWatcher] Already running")
            return
        }

        println("[FileWatcher] Starting file watcher for: ${directory.absolutePath}")
        watchJob = scope.launch(Dispatchers.IO) {
            watchLoop()
        }
    }

    /**
     * Stop watching and clean up resources.
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }

        println("[FileWatcher] Stopping file watcher")
        watchJob?.cancel()
        debounceJob?.cancel()

        // Close all watch keys
        watchedDirectories.values.forEach { key ->
            key.cancel()
        }
        watchedDirectories.clear()

        try {
            watchService.close()
        } catch (e: Exception) {
            println("[FileWatcher] Error closing watch service: ${e.message}")
        }

        _debouncedChanges.value = false
    }

    /**
     * Main watch loop that processes file system events.
     */
    private suspend fun watchLoop() {
        try {
            while (isRunning.get() && isActive()) {
                // take() blocks until an event is available
                val key: WatchKey = try {
                    withContext(Dispatchers.IO) {
                        watchService.take()
                    }
                } catch (e: ClosedWatchServiceException) {
                    println("[FileWatcher] WatchService closed")
                    break
                } catch (e: InterruptedException) {
                    println("[FileWatcher] WatchService interrupted")
                    break
                } catch (e: CancellationException) {
                    println("[FileWatcher] WatchService cancelled")
                    break
                }

                val eventPath = key.watchable() as? Path

                for (event in key.pollEvents()) {
                    val kind = event.kind()

                    // Skip OVERFLOW events
                    if (kind == OVERFLOW) {
                        continue
                    }

                    val contextPath = event.context() as? Path
                    if (contextPath == null || eventPath == null) continue

                    val fullPath = eventPath.resolve(contextPath)

                    // Filter by file extension
                    if (!shouldWatchFile(fullPath.toFile())) {
                        continue
                    }

                    val changeType = when (kind.name()) {
                        "ENTRY_CREATE" -> ChangeType.CREATE
                        "ENTRY_MODIFY" -> ChangeType.MODIFY
                        "ENTRY_DELETE" -> ChangeType.DELETE
                        else -> continue
                    }

                    val changeEvent = FileChangeEvent(
                        file = fullPath.toFile(),
                        type = changeType,
                        relativePath = directory.toPath().relativize(fullPath).toString()
                    )

                    println("[FileWatcher] Detected: $changeType on ${fullPath.fileName}")

                    // Emit raw event
                    _events.emit(changeEvent)

                    // Trigger debounced notification
                    triggerDebouncedNotification()
                }

                // Reset key to receive further events
                if (!key.reset()) {
                    println("[FileWatcher] WatchKey no longer valid, directory may have been deleted")
                    watchedDirectories.remove(key.watchable() as? Path)
                    break
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            println("[FileWatcher] Error in watch loop: ${e.message}")
        }
    }

    /**
     * Check if a file should be watched based on its extension.
     */
    private fun shouldWatchFile(file: File): Boolean {
        if (fileExtensions.isEmpty()) return true
        val fileName = file.name
        val dotIndex = fileName.lastIndexOf('.')
        if (dotIndex < 0) return false
        val extension = fileName.substring(dotIndex + 1).lowercase()
        return extension in fileExtensions
    }

    /**
     * Check if coroutine is still active.
     */
    private fun isActive(): Boolean = scope.coroutineContext[Job]?.isActive == true

    /**
     * Trigger a debounced notification.
     * Resets a timer each time a change is detected, and only
     * signals true after [debounceMs] of no further changes.
     */
    private fun triggerDebouncedNotification() {
        debounceJob?.cancel()
        _debouncedChanges.value = false

        debounceJob = scope.launch {
            delay(debounceMs)
            if (isRunning.get()) {
                _debouncedChanges.value = true
                // Reset after a brief period so subsequent changes can trigger again
                delay(500L)
                _debouncedChanges.value = false
            }
        }
    }

    /**
     * Manually trigger a sync (e.g., for testing or forced sync).
     */
    fun notifyChanged() {
        triggerDebouncedNotification()
    }
}

/**
 * Represents a file system change event.
 */
data class FileChangeEvent(
    val file: File,
    val type: ChangeType,
    val relativePath: String
)

/**
 * Type of file system change.
 */
enum class ChangeType {
    CREATE,
    MODIFY,
    DELETE
}