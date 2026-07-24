package com.darknote.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darknote.core.backup.BackupService
import com.darknote.core.backup.ImportSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State of the last backup export/import operation, shown in Settings.
 */
sealed class BackupState {
    data object Idle : BackupState()
    data object Exporting : BackupState()
    data object Importing : BackupState()
    data object ExportSuccess : BackupState()
    data class ImportSuccess(val summary: ImportSummary) : BackupState()
    data class Error(val message: String) : BackupState()
}

/**
 * ViewModel for the manual backup/restore feature.
 *
 * This is independent of Dropbox sync — it exports the entire local
 * library (folders + snippets with full content) to a single JSON file
 * the user picks the location for via the system file picker (Storage
 * Access Framework), and can restore it the same way. The same
 * [BackupService] and JSON format work on Desktop too, so a backup made
 * here can be restored on the Desktop app.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupService: BackupService
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    /**
     * Exports the library to JSON and hands it to [writeText] (typically a
     * write to a Uri obtained via ActivityResultContracts.CreateDocument).
     */
    fun exportBackup(writeText: suspend (String) -> Unit) {
        viewModelScope.launch {
            _backupState.value = BackupState.Exporting
            try {
                val json = backupService.exportToJson()
                writeText(json)
                _backupState.value = BackupState.ExportSuccess
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Export failed")
            }
        }
    }

    /**
     * Restores the library from JSON obtained via [readText] (typically a
     * read from a Uri obtained via ActivityResultContracts.OpenDocument).
     */
    fun importBackup(readText: suspend () -> String) {
        viewModelScope.launch {
            _backupState.value = BackupState.Importing
            try {
                val text = readText()
                val result = backupService.importFromJson(text)
                _backupState.value = if (result.isSuccess) {
                    BackupState.ImportSuccess(result.getOrThrow())
                } else {
                    BackupState.Error(result.exceptionOrNull()?.message ?: "Import failed")
                }
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun dismiss() {
        _backupState.value = BackupState.Idle
    }
}
