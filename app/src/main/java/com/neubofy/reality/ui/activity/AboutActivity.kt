package com.neubofy.reality.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.neubofy.reality.R
import com.neubofy.reality.ui.base.BaseActivity
import com.neubofy.reality.ui.theme.RealityTheme
import com.neubofy.reality.utils.TerminalLogger
import com.neubofy.reality.utils.UpdateManager
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

class AboutActivity : BaseActivity() {

    companion object {
        const val ABOUT_MD_URL = "https://raw.githubusercontent.com/neubofy/Reality/main/ABOUT.md"
        const val GITHUB_PROFILE = "https://github.com/pawanwashudev-official"
        const val GITHUB_REPO = "https://github.com/neubofy/Reality"
        const val TELEGRAM = "https://t.me/pawanwashudev"
        const val WHATSAPP = "https://wa.me/pawanwashudev"
        const val INSTAGRAM = "https://instagram.com/pawanwashudev"
        const val LINKEDIN = "https://linkedin.com/in/pawanwashudev"
        const val EMAIL = "support@neubofy.in"
        const val PRIVACY_POLICY = "https://reality.neubofy.in/privacypolicy"
        const val WEBSITE = "https://reality.neubofy.in"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var versionName = "Unknown"
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionName = pInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            TerminalLogger.log("ERROR: ${e.message}")
        }

        setContent {
            RealityTheme {
                AboutScreen(
                    versionName = versionName,
                    onBackClick = { finish() },
                    onUrlClick = { openUrl(it) },
                    onEmailClick = { handleEmailClick() },
                    onCheckUpdate = { isBeta, onComplete -> checkUpdate(isBeta, onComplete) },
                    onReportIssue = { title, desc -> reportIssue(title, desc) }
                )
            }
        }
    }

    private fun checkUpdate(isBeta: Boolean, onComplete: () -> Unit) {
        UpdateManager.checkForUpdates(this, silent = false, isBeta = isBeta, onCheckComplete = {
            runOnUiThread {
                onComplete()
            }
        })
    }

    private fun reportIssue(titleBoxText: String, descBoxText: String) {
        val title = URLEncoder.encode(titleBoxText, "UTF-8")
        val body = URLEncoder.encode(descBoxText, "UTF-8")
        val url = "https://github.com/neubofy/Reality/issues/new?title=$title&body=$body"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleEmailClick() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "Reality App - Support Request")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Email", EMAIL)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onBackClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    onEmailClick: () -> Unit,
    onCheckUpdate: (Boolean, () -> Unit) -> Unit,
    onReportIssue: (String, String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Reality") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Reality",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ActionCard(
                    title = "Check for Updates",
                    subtitle = "Keep Reality at its best",
                    iconRes = R.drawable.baseline_info_24,
                    onClick = {
                        // The original logic handles its own UI updates, but here we can manage a local state if needed.
                        // For simplicity and matching original flow, we just call it.
                    }
                ) {
                    var checking by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!checking) {
                                    checking = true
                                    onCheckUpdate(false) { checking = false }
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.baseline_info_24), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Check for Updates", style = MaterialTheme.typography.titleMedium)
                                Text(if (checking) "Checking for updates..." else "Keep Reality at its best", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (checking) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }

            item {
                ActionCard(
                    title = "Beta Updates",
                    subtitle = "Get early access features",
                    iconRes = R.drawable.baseline_bug_report_24, // Assuming this exists, or use something else.
                    onClick = {}
                ) {
                    var checking by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!checking) {
                                    checking = true
                                    onCheckUpdate(true) { checking = false }
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.baseline_bug_report_24), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Beta Updates", style = MaterialTheme.typography.titleMedium)
                                Text(if (checking) "Checking for beta updates..." else "Get early access features", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (checking) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
            
            item {
                var showDialog by remember { mutableStateOf(false) }
                if (showDialog) {
                    ReportIssueDialog(
                        onDismiss = { showDialog = false },
                        onSubmit = { title, desc ->
                            showDialog = false
                            onReportIssue(title, desc)
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.baseline_bug_report_24), contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Report an Issue", style = MaterialTheme.typography.titleMedium)
                            Text("Help us improve Reality", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column {
                        Text(
                            "Connect & Support",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                        )
                        SocialRow("Website", R.drawable.baseline_language_24) { onUrlClick(AboutActivity.WEBSITE) }
                        SocialRow("Developer GitHub", R.drawable.baseline_language_24) { onUrlClick(AboutActivity.GITHUB_PROFILE) }
                        SocialRow("Email Support", R.drawable.baseline_email_24) { onEmailClick() }
                        SocialRow("WhatsApp Support", R.drawable.baseline_chat_24) { onUrlClick(AboutActivity.WHATSAPP) }
                        SocialRow("Developer Telegram", R.drawable.baseline_send_24) { onUrlClick(AboutActivity.TELEGRAM) }
                        SocialRow("Instagram Support", R.drawable.baseline_language_24) { onUrlClick(AboutActivity.INSTAGRAM) }
                        SocialRow("LinkedIn Support", R.drawable.baseline_language_24) { onUrlClick(AboutActivity.LINKEDIN) }
                    }
                }
            }

            item {
                 MarkdownContent()
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Source-Available", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                    Text(
                        "View Source Code on GitHub",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { onUrlClick(AboutActivity.GITHUB_REPO) }.padding(8.dp)
                    )
                    Text(
                        "Privacy Policy",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onUrlClick(AboutActivity.PRIVACY_POLICY) }.padding(8.dp)
                    )
                    Text("© 2026 Neubofy. All rights reserved.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
                    Text("Made with ❤️ in India", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, iconRes: Int, onClick: () -> Unit, content: (@Composable () -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        if (content != null) {
            content()
        } else {
            Row(
                modifier = Modifier
                    .clickable { onClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SocialRow(text: String, iconRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ReportIssueDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report an Issue") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Issue Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Issue Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(title, desc) }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MarkdownContent() {
    val context = LocalContext.current
    var markdownContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("about_cache", Context.MODE_PRIVATE)
        markdownContent = prefs.getString("markdown_content", null)

        withContext(Dispatchers.IO) {
            try {
                val content = URL(AboutActivity.ABOUT_MD_URL).readText()
                withContext(Dispatchers.Main) {
                    markdownContent = content
                    prefs.edit().putString("markdown_content", content).apply()
                }
            } catch (e: Exception) {
                TerminalLogger.log("ERROR: ${e.message}")
            }
        }
    }

    if (markdownContent != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            AndroidView(
                modifier = Modifier.padding(16.dp),
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextColor(android.graphics.Color.GRAY) // Base color, markwon applies styles
                    }
                },
                update = { textView ->
                    val markwon = Markwon.builder(context).build()
                    markwon.setMarkdown(textView, markdownContent ?: "")
                }
            )
        }
    }
}
