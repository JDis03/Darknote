package com.darknote.desktop.viewmodel

import com.darknote.core.backup.BackupService
import com.darknote.core.backup.ImportSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.io.File

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
 * ViewModel for the manual backup/restore feature on Desktop.
 *
 * Uses the same [BackupService] and portable JSON format as Android, so a
 * backup exported from the Android app can be restored here, and vice versa.
 * Independent of Dropbox sync — works fully offline via a native file dialog.
 */
class BackupViewModel(
    private val backupService: BackupService
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    /** Opens a native "Save As" dialog and exports the library there. */
    fun exportBackup() {
        scope.launch {
            _backupState.value = BackupState.Exporting
            try {
                val file = chooseSaveFile("darknote-backup-${timestampForFilename()}.json")
                if (file == null) {
                    _backupState.value = BackupState.Idle
                    return@launch
                }
                val json = backupService.exportToJson()
                withContext(Dispatchers.IO) { file.writeText(json) }
                _backupState.value = BackupState.ExportSuccess
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Export failed")
            }
        }
    }

    /** Opens a native "Open" dialog and restores the library from the chosen file. */
    fun importBackup() {
        scope.launch {
            _backupState.value = BackupState.Importing
            try {
                val file = chooseOpenFile()
                if (file == null) {
                    _backupState.value = BackupState.Idle
                    return@launch
                }
                val text = withContext(Dispatchers.IO) { file.readText() }
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

    private suspend fun chooseSaveFile(suggestedName: String): File? = withContext(Dispatchers.IO) {
        val dialog = FileDialog(null as java.awt.Frame?, "Export Backup", FileDialog.SAVE)
        dialog.file = suggestedName
        dialog.isVisible = true
        val name = dialog.file ?: return@withContext null
        File(dialog.directory, name)
    }

    private suspend fun chooseOpenFile(): File? = withContext(Dispatchers.IO) {
        val dialog = FileDialog(null as java.awt.Frame?, "Restore Backup", FileDialog.LOAD)
        dialog.isVisible = true
        val name = dialog.file ?: return@withContext null
        File(dialog.directory, name)
    }

    private fun timestampForFilename(): String {
        val now = java.time.LocalDateTime.now()
        return java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(now)
    }
}
