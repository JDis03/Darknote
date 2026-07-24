package com.darknote.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.darknote.desktop.settings.ThemeMode
import com.darknote.desktop.viewmodel.AuthState
import com.darknote.desktop.viewmodel.BackupState

/**
 * Settings dialog for app preferences: theme + Dropbox connection + backup.
 */
@Composable
fun SettingsDialog(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    authState: AuthState,
    authUrl: String?,
    onConnectDropbox: () -> Unit,
    onCompleteAuth: (String) -> Unit,
    onDisconnectDropbox: () -> Unit,
    backupState: BackupState,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onDismissBackupResult: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Theme", style = MaterialTheme.typography.titleSmall)

                Column(Modifier.selectableGroup()) {
                    ThemeOption(
                        label = "Dark",
                        selected = currentThemeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) }
                    )
                    ThemeOption(
                        label = "Light",
                        selected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                    )
                    ThemeOption(
                        label = "System (Follow KDE)",
                        selected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                    )
                }

                HorizontalDivider()

                Text("Dropbox Sync", style = MaterialTheme.typography.titleSmall)

                DropboxSection(
                    authState = authState,
                    authUrl = authUrl,
                    onConnect = onConnectDropbox,
                    onCompleteAuth = onCompleteAuth,
                    onDisconnect = onDisconnectDropbox
                )

                HorizontalDivider()

                Text("Backup & Restore", style = MaterialTheme.typography.titleSmall)

                BackupSection(
                    backupState = backupState,
                    onExport = onExportBackup,
                    onImport = onImportBackup
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Backup result dialog (separate from the Settings dialog so it's visible on top of it)
    when (backupState) {
        is BackupState.ExportSuccess -> {
            AlertDialog(
                onDismissRequest = onDismissBackupResult,
                title = { Text("Backup saved") },
                text = { Text("Your folders and snippets were exported successfully.") },
                confirmButton = { TextButton(onClick = onDismissBackupResult) { Text("OK") } }
            )
        }
        is BackupState.ImportSuccess -> {
            val s = backupState.summary
            AlertDialog(
                onDismissRequest = onDismissBackupResult,
                title = { Text("Backup restored") },
                text = {
                    Column {
                        Text("Folders: ${s.foldersImported} added, ${s.foldersUpdated} updated")
                        Text("Snippets: ${s.snippetsImported} added, ${s.snippetsUpdated} updated")
                        if (s.errors.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("${s.errors.size} item(s) skipped:", color = MaterialTheme.colorScheme.error)
                            s.errors.forEach { err ->
                                Text("• $err", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismissBackupResult) { Text("OK") } }
            )
        }
        is BackupState.Error -> {
            AlertDialog(
                onDismissRequest = onDismissBackupResult,
                title = { Text("Backup failed") },
                text = { Text(backupState.message) },
                confirmButton = { TextButton(onClick = onDismissBackupResult) { Text("OK") } }
            )
        }
        else -> {}
    }
}

@Composable
private fun BackupSection(
    backupState: BackupState,
    onExport: () -> Unit,
    onImport: () -> Unit
) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        "Save all your folders and snippets to a single file you control, independent of " +
            "Dropbox. The same file can be restored on the Android app.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val isBusy = backupState is BackupState.Exporting || backupState is BackupState.Importing

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onExport, enabled = !isBusy) {
            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export Backup")
        }
        OutlinedButton(onClick = onImport, enabled = !isBusy) {
            Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Restore Backup")
        }
    }

    if (isBusy) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                if (backupState is BackupState.Exporting) "Exporting..." else "Restoring...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DropboxSection(
    authState: AuthState,
    authUrl: String?,
    onConnect: () -> Unit,
    onCompleteAuth: (String) -> Unit,
    onDisconnect: () -> Unit
) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    when (authState) {
        is AuthState.Idle -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Checking connection...", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is AuthState.NotAuthenticated -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Not connected", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onConnect) {
                Text("Connect to Dropbox")
            }
        }

        is AuthState.Authorizing -> {
            var code by remember { mutableStateOf("") }
            val clipboard: ClipboardManager = LocalClipboardManager.current
            var linkCopied by remember { mutableStateOf(false) }

            Text(
                "A browser window should have opened. Approve access, then paste the code Dropbox gives you below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (authUrl != null) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(authUrl))
                        linkCopied = true
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (linkCopied) "Link copied!" else "Didn't open automatically? Copy link")
                }
            }
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Authorization code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onCompleteAuth(code) },
                enabled = code.isNotBlank()
            ) {
                Text("Submit")
            }
        }

        is AuthState.Authenticated -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Connected to Dropbox", style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = onDisconnect) {
                Text("Disconnect")
            }
        }

        is AuthState.Error -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    authState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(onClick = onConnect) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
