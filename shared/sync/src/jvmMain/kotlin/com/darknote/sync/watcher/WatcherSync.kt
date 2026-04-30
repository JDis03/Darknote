package com.darknote.sync.watcher

import com.darknote.sync.engine.SyncEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Integrates FileWatcher with SyncEngine to provide automatic sync
 * when local snippet files change.
 *
 * Usage:
 *   val watcherSync = WatcherSync(syncEngine, scope)
 *   watcherSync.start()  // Begins watching and auto-syncing
 *   // ...
 *   watcherSync.stop()   // Clean shutdown
 */
class WatcherSync(
    private val syncEngine: SyncEngine,
    private val scope: CoroutineScope,
    private val snippetsDir: java.io.File = java.io.File(System.getProperty("user.home"), ".config/darknote/snippets"),
    private val debounceMs: Long = 2000L
) {
    private var fileWatcher: FileWatcher? = null
    private var syncJob: Job? = null

    // Exposed state for UI
    private val _isWatching = MutableStateFlow(false)
    val isWatching: StateFlow<Boolean> = _isWatching.asStateFlow()

    private val _lastSyncTrigger = MutableStateFlow(0L)
    val lastSyncTrigger: StateFlow<Long> = _lastSyncTrigger.asStateFlow()

    /**
     * Start watching for file changes and auto-syncing.
     * Only starts if Dropbox is authorized; otherwise, starts watching
     * but sync will not be triggered until authorization.
     */
    fun start() {
        if (fileWatcher != null) {
            println("[WatcherSync] Already started")
            return
        }

        fileWatcher = FileWatcher(
            directory = snippetsDir,
            scope = scope,
            debounceMs = debounceMs
        )

        // Start the file watcher
        fileWatcher!!.start()
        _isWatching.value = true

        // Observe debounced changes and trigger sync
        syncJob = scope.launch {
            fileWatcher!!.debouncedChanges
                .filter { it }
                .collect {
                    println("[WatcherSync] File change detected, triggering sync...")
                    _lastSyncTrigger.value = System.currentTimeMillis()
                    try {
                        val result = syncEngine.sync()
                        if (result.isSuccess) {
                            val syncResult = result.getOrNull()
                            println("[WatcherSync] Auto-sync completed: uploaded=${syncResult?.uploaded}, downloaded=${syncResult?.downloaded}")
                        } else {
                            println("[WatcherSync] Auto-sync failed: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        println("[WatcherSync] Auto-sync error: ${e.message}")
                    }
                }
        }

        println("[WatcherSync] Started watching: ${snippetsDir.absolutePath}")
    }

    /**
     * Stop watching and clean up all resources.
     */
    fun stop() {
        syncJob?.cancel()
        syncJob = null
        fileWatcher?.stop()
        fileWatcher = null
        _isWatching.value = false
        println("[WatcherSync] Stopped")
    }

    /**
     * Check if currently watching.
     */
    val isRunning: Boolean get() = _isWatching.value
}