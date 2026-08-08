package com.neubofy.reality.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.reality.google.GoogleAuthManager
import com.neubofy.reality.ui.base.BaseActivity
import com.neubofy.reality.ui.theme.RealityTheme
import com.neubofy.reality.utils.BackupManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreActivity : BaseActivity() {

    private var cachedBackupInfo by mutableStateOf<BackupManager.BackupInfo?>(null)
    private var inProgress by mutableStateOf(false)
    private var progressText by mutableStateOf("")
    private var progressValue by mutableStateOf(0f)
    private var isSignedIn by mutableStateOf(false)
    private var signedInEmail by mutableStateOf("")
    private var isLoadingBackup by mutableStateOf(false)
    private var backupError by mutableStateOf<String?>(null)

    // Using maps for toggles directly in state
    private val backupTogglesState = mutableStateMapOf<BackupManager.BackupCategory, Boolean>()
    private val restoreTogglesState = mutableStateMapOf<BackupManager.BackupCategory, Boolean>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize backup toggles to true
        BackupManager.BackupCategory.values().forEach {
            backupTogglesState[it] = true
        }

        setContent {
            RealityTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Backup & Restore") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        BackupRestoreScreen()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkSignInStatus()
        loadBackupInfo()
    }

    private fun checkSignInStatus() {
        isSignedIn = GoogleAuthManager.isFullWorkspaceConnected(this)
        if (isSignedIn) {
            signedInEmail = GoogleAuthManager.getUserEmail(this) ?: ""
        }
    }

    private fun loadBackupInfo() {
        if (!GoogleAuthManager.isFullWorkspaceConnected(this)) return

        isLoadingBackup = true
        backupError = null

        lifecycleScope.launch {
            try {
                val info = BackupManager.getBackupInfo(this@BackupRestoreActivity)

                runOnUiThread {
                    cachedBackupInfo = info
                    isLoadingBackup = false

                    if (info.exists) {
                        info.categories.mapNotNull { name ->
                            try { BackupManager.BackupCategory.valueOf(name) } catch (_: Exception) { null }
                        }.forEach {
                            if (!restoreTogglesState.containsKey(it)) {
                                restoreTogglesState[it] = true
                            }
                        }
                    }
                }
            } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                runOnUiThread {
                    isLoadingBackup = false
                    startActivity(e.intent)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    isLoadingBackup = false
                    backupError = "⚠️ Could not check backup status"
                }
            }
        }
    }

    private fun performBackup(categories: Set<BackupManager.BackupCategory>) {
        setOperationInProgress(true, "Preparing backup...")

        lifecycleScope.launch {
            try {
                val result = BackupManager.createBackup(
                    this@BackupRestoreActivity,
                    categories
                ) { progress, status ->
                    runOnUiThread {
                        progressValue = progress
                        progressText = status
                    }
                }

                runOnUiThread {
                    setOperationInProgress(false)
                    if (result.success) {
                        Toast.makeText(this@BackupRestoreActivity, "✅ ${result.message}", Toast.LENGTH_LONG).show()
                        loadBackupInfo()
                    } else if (result.message.startsWith("NEED_PERMISSION:")) {
                        Toast.makeText(this@BackupRestoreActivity, "Please grant Google Drive access", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@BackupRestoreActivity, "❌ ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                runOnUiThread {
                    setOperationInProgress(false)
                    startActivity(e.intent)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setOperationInProgress(false)
                    Toast.makeText(this@BackupRestoreActivity, "❌ Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showRestoreConfirmation(categories: Set<BackupManager.BackupCategory>) {
        val catNames = categories.joinToString("\n") { "  • ${it.icon} ${it.displayName}" }
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Restore from Backup?")
            .setMessage("The following categories will be restored, overwriting current data:\n\n$catNames\n\nThis cannot be undone. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                performRestore(categories)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRestore(categories: Set<BackupManager.BackupCategory>) {
        setOperationInProgress(true, "Downloading backup...")

        lifecycleScope.launch {
            try {
                val result = BackupManager.restoreBackup(
                    context = this@BackupRestoreActivity,
                    categories = categories
                ) { progress, status ->
                    runOnUiThread {
                        progressValue = progress
                        progressText = status
                    }
                }

                runOnUiThread {
                    setOperationInProgress(false)
                    if (result.success) {
                        MaterialAlertDialogBuilder(this@BackupRestoreActivity)
                            .setTitle("✅ Restore Complete")
                            .setMessage("${result.message}\n\nPlease restart the app for all changes to take effect.")
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    } else {
                        Toast.makeText(this@BackupRestoreActivity, "❌ Restore failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                runOnUiThread {
                    setOperationInProgress(false)
                    startActivity(e.intent)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setOperationInProgress(false)
                    Toast.makeText(this@BackupRestoreActivity, "❌ Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setOperationInProgress(active: Boolean, status: String = "") {
        inProgress = active
        progressValue = 0f
        progressText = status
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    @Composable
    fun BackupRestoreScreen() {
        val context = LocalContext.current

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sign in prompt
            if (!isSignedIn) {
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️ Google Drive Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("You need to sign in with Google to backup or restore data.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                val intent = Intent(context, ProfileActivity::class.java)
                                context.startActivity(intent)
                            }) {
                                Text("Sign In")
                            }
                        }
                    }
                }
            } else {
                item {
                    Text("Signed in as: $signedInEmail", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                }
            }

            // Backup Card
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Create Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Save your settings and schedules to Google Drive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Text("Select what to backup:", style = MaterialTheme.typography.labelLarge)
                        BackupManager.BackupCategory.values().forEach { category ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                backupTogglesState[category] = !(backupTogglesState[category] ?: true)
                            }.padding(vertical = 4.dp)) {
                                Checkbox(
                                    checked = backupTogglesState[category] ?: true,
                                    onCheckedChange = { backupTogglesState[category] = it },
                                    enabled = !inProgress
                                )
                                Text("${category.icon} ${category.displayName}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val selected = backupTogglesState.filter { it.value }.keys
                                performBackup(selected)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isSignedIn && !inProgress && backupTogglesState.any { it.value }
                        ) {
                            Text("Backup Now")
                        }
                    }
                }
            }

            // Restore Card
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Restore from Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Download and restore your saved data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        if (isLoadingBackup) {
                            Text("🔄 Checking for backups...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        } else if (backupError != null) {
                            Text(backupError!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        } else if (cachedBackupInfo?.exists == true) {
                            val info = cachedBackupInfo!!
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

                            Text("Last Backup", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                            Text("📅  ${dateFormat.format(Date(info.timestamp))}", style = MaterialTheme.typography.bodyMedium)
                            Text("📦  ${formatSize(info.sizeBytes)}", style = MaterialTheme.typography.bodyMedium)
                            Text("📱  App v${info.appVersion}", style = MaterialTheme.typography.bodyMedium)

                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                            Text("Select what to restore:", style = MaterialTheme.typography.labelLarge)

                            restoreTogglesState.keys.forEach { category ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                    restoreTogglesState[category] = !(restoreTogglesState[category] ?: true)
                                }.padding(vertical = 4.dp)) {
                                    Checkbox(
                                        checked = restoreTogglesState[category] ?: true,
                                        onCheckedChange = { restoreTogglesState[category] = it },
                                        enabled = !inProgress
                                    )
                                    Text("${category.icon} ${category.displayName}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val selected = restoreTogglesState.filter { it.value }.keys
                                    showRestoreConfirmation(selected)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isSignedIn && !inProgress && restoreTogglesState.any { it.value }
                            ) {
                                Text("Restore Now")
                            }

                        } else {
                            Text("No backup found on Google Drive", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Progress Card
            if (inProgress) {
                item {
                    GlassCard {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(progress = { progressValue }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(progressText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Footer
            item {
                Text(
                    "🔒 Your backup is stored in a hidden folder on Google Drive. Only this app can access it. Each backup replaces the previous one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(content = content)
    }
}
