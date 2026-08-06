package com.neubofy.reality.ui.activity

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.neubofy.reality.R
import com.neubofy.reality.ui.base.BaseActivity
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

        var versionName = "Version Unknown"
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionName = "Version ${pInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            TerminalLogger.log("ERROR: ${e.message}")
        }

        setContent {
            MaterialTheme {
                AboutScreen(versionName)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AboutScreen(versionName: String) {
        val context = LocalContext.current
        var updateStatus by remember { mutableStateOf("Keep Reality at its best") }
        var isUpdating by remember { mutableStateOf(false) }
        
        var betaUpdateStatus by remember { mutableStateOf("Get early access features") }
        var isBetaUpdating by remember { mutableStateOf(false) }

        var markdownContent by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            val prefs = getSharedPreferences("about_cache", Context.MODE_PRIVATE)
            val cachedContent = prefs.getString("markdown_content", null)
            if (cachedContent != null) {
                markdownContent = cachedContent
            }
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val content = URL(ABOUT_MD_URL).readText()
                    withContext(Dispatchers.Main) {
                        markdownContent = content
                        prefs.edit().putString("markdown_content", content).apply()
                    }
                } catch (e: Exception) {
                    TerminalLogger.log("ERROR: ${e.message}")
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(96.dp)
                )

                Text(
                    text = "Reality",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = versionName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Actions Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        ActionItem(
                            iconRes = R.drawable.baseline_system_update_24,
                            title = "Check for Updates",
                            subtitle = updateStatus,
                            isLoading = isUpdating
                        ) {
                            isUpdating = true
                            updateStatus = "Checking for updates..."
                            UpdateManager.checkForUpdates(this@AboutActivity, silent = false, isBeta = false, onCheckComplete = {
                                runOnUiThread {
                                    isUpdating = false
                                    updateStatus = "Keep Reality at its best"
                                }
                            })
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        ActionItem(
                            iconRes = R.drawable.baseline_auto_awesome_24,
                            title = "Check for Beta Updates",
                            subtitle = betaUpdateStatus,
                            isLoading = isBetaUpdating
                        ) {
                            isBetaUpdating = true
                            betaUpdateStatus = "Checking for beta updates..."
                            UpdateManager.checkForUpdates(this@AboutActivity, silent = false, isBeta = true, onCheckComplete = {
                                runOnUiThread {
                                    isBetaUpdating = false
                                    betaUpdateStatus = "Get early access features"
                                }
                            })
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        ActionItem(
                            iconRes = R.drawable.baseline_bug_report_24,
                            title = "Raise an Issue",
                            subtitle = "Report bugs or request features"
                        ) {
                            // Minimal implementation for raise issue using browser intent directly
                            val url = "https://github.com/neubofy/Reality/issues/new"
                            openUrl(url)
                        }
                    }
                }

                // Community Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Community & Support",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        ContactItem(iconRes = R.drawable.baseline_language_24, text = "Official Website") { openUrl(WEBSITE) }
                        ContactItem(iconRes = R.drawable.baseline_code_24, text = "Developer GitHub") { openUrl(GITHUB_PROFILE) }
                        ContactItem(iconRes = R.drawable.baseline_email_24, text = "Email Support") {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$EMAIL")
                                putExtra(Intent.EXTRA_SUBJECT, "Reality App - Support Request")
                            }
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Email", EMAIL)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                        ContactItem(iconRes = R.drawable.baseline_chat_24, text = "WhatsApp Support") { openUrl(WHATSAPP) }
                        ContactItem(iconRes = R.drawable.baseline_send_24, text = "Developer Telegram") { openUrl(TELEGRAM) }
                        ContactItem(iconRes = R.drawable.baseline_language_24, text = "Instagram Support") { openUrl(INSTAGRAM) }
                        ContactItem(iconRes = R.drawable.baseline_language_24, text = "LinkedIn Support") { openUrl(LINKEDIN) }
                    }
                }

                // Markdown Content Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { ctx ->
                                TextView(ctx).apply {
                                    setTextColor(android.graphics.Color.GRAY)
                                }
                            },
                            update = { view ->
                                val markwon = Markwon.builder(context).build()
                                markwon.setMarkdown(view, markdownContent)
                            }
                        )
                    }
                }

                // Footer
                Text(
                    text = "Source-Available",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "View Source Code on GitHub",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { openUrl(GITHUB_REPO) }
                        .padding(8.dp)
                )

                Text(
                    text = "Privacy Policy",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { openUrl(PRIVACY_POLICY) }
                        .padding(12.dp, top = 24.dp)
                )

                Text(
                    text = "© 2026 Neubofy. All rights reserved.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "Made with ❤️ in India",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    @Composable
    fun ActionItem(iconRes: Int, title: String, subtitle: String, isLoading: Boolean = false, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    @Composable
    fun ContactItem(iconRes: Int, text: String, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
            Text(
                text = text,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp)
            )
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
