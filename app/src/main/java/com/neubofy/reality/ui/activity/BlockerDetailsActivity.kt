package com.neubofy.reality.ui.activity

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.neubofy.reality.Constants
import com.neubofy.reality.R
import com.neubofy.reality.ui.base.BaseActivity
import com.neubofy.reality.ui.theme.RealityTheme
import com.neubofy.reality.utils.BlockCache
import com.neubofy.reality.utils.SavedPreferencesLoader
import com.neubofy.reality.services.TapasyaManager
import java.text.SimpleDateFormat
import java.util.*

class BlockerDetailsActivity : BaseActivity() {

    private lateinit var savedPreferencesLoader: SavedPreferencesLoader

    data class AppItem(val packageName: String, val name: String, val reasons: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedPreferencesLoader = SavedPreferencesLoader(this)

        setContent {
            RealityTheme {
                BlockerDetailsScreen()
            }
        }
    }

    private fun isBedtimeActiveNow(bedtime: Constants.BedtimeData): Boolean {
        if (!bedtime.isEnabled) return false
        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = bedtime.startTimeInMins
        val end = bedtime.endTimeInMins
        return if (start < end) {
            currentMins in start until end
        } else if (start > end) {
            currentMins >= start || currentMins < end
        } else {
            false
        }
    }

    private fun formatMinsToTime(minutes: Int): String {
        val hour = minutes / 60
        val min = minutes % 60
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%02d %s", displayHour, min, ampm)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BlockerDetailsScreen() {
        val context = LocalContext.current

        // 1. System Status
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isDndActive = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val bedtimeData = savedPreferencesLoader.getBedtimeData()
        val isBedtimeActive = isBedtimeActiveNow(bedtimeData)

        // 2. Active Blocker Sessions
        val now = System.currentTimeMillis()
        val tapasyaState = TapasyaManager.getCurrentState(context)
        val manualBlocker = savedPreferencesLoader.getFocusModeData()

        val activeBlockers = mutableListOf<Triple<String, String, String>>() // Title, Reason, EndTime, Color is handled via composable

        if (tapasyaState.isSessionActive) {
            activeBlockers.add(Triple("Tapasya Mode", "All target apps blocked to maximize focus & build discipline.", "Active until stopped"))
        }

        if (isBedtimeActive) {
            val endStr = formatMinsToTime(bedtimeData.endTimeInMins)
            activeBlockers.add(Triple("Bedtime Mode", "Bedtime schedule is active. Sleeping hours protected.", "Ends at $endStr"))
        }

        if (manualBlocker.isTurnedOn && manualBlocker.endTime > now && !manualBlocker.isTapasyaTriggered) {
            val df = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val endStr = df.format(Date(manualBlocker.endTime))
            activeBlockers.add(Triple("Manual Blocker Session", "You started a manual blocking session to prevent distraction.", "Ends at $endStr"))
        }

        // 3. Target Apps
        val appList = remember { mutableStateListOf<AppItem>() }
        LaunchedEffect(Unit) {
            val activeBlockedMap = BlockCache.getAllBlockedApps()
            val tempList = mutableListOf<AppItem>()
            val pm = context.packageManager

            if (activeBlockedMap.isNotEmpty()) {
                for ((pkg, reasons) in activeBlockedMap) {
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val label = pm.getApplicationLabel(appInfo).toString()
                        tempList.add(AppItem(pkg, label, reasons.joinToString(", ")))
                    } catch (e: PackageManager.NameNotFoundException) {}
                }
            } else {
                val focusApps = savedPreferencesLoader.getFocusModeSelectedApps()
                for (pkg in focusApps) {
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val label = pm.getApplicationLabel(appInfo).toString()
                        tempList.add(AppItem(pkg, label, "Focus Mode List (Inactive)"))
                    } catch (e: PackageManager.NameNotFoundException) {}
                }
            }
            tempList.sortBy { it.name.lowercase() }
            appList.addAll(tempList)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Blocker Details") },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("System Features Status", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                            StatusRow("Do Not Disturb (DND)", isDndActive, R.drawable.baseline_do_not_disturb_on_24)
                            StatusRow("Dark Mode", isDarkMode, R.drawable.baseline_brightness_4_24)
                            StatusRow("Wallpaper Dimming / Sleep Mode", isBedtimeActive, R.drawable.baseline_bedtime_24, activeText = "Active (Reality Sleep)")
                        }
                    }
                }

                item {
                    Text("Active Blockers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                if (activeBlockers.isEmpty()) {
                    item {
                        Text("No active blocker sessions right now.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                    }
                } else {
                    items(activeBlockers) { blocker ->
                        val colorHex = when(blocker.first) {
                            "Tapasya Mode" -> Color(0xFF9C27B0)
                            "Bedtime Mode" -> Color(0xFFE91E63)
                            else -> Color(0xFF2196F3)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(2.dp, colorHex),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(blocker.first, color = colorHex, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(blocker.second, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                                Text(blocker.third, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                            }
                        }
                    }
                }

                item {
                    Text("Target Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (appList.isEmpty()) {
                                Text("No blocked apps configured.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
                            } else {
                                appList.forEach { app ->
                                    val isCurrentlyBlocked = BlockCache.getAllBlockedApps().containsKey(app.packageName)
                                    val statusText = if (isCurrentlyBlocked) "BLOCKED" else "MONITORED"
                                    val statusColor = if (isCurrentlyBlocked) Color(0xFFFF5252) else Color(0xFF4CAF50)

                                    var iconDrawable by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
                                    LaunchedEffect(app.packageName) {
                                        try {
                                            iconDrawable = context.packageManager.getApplicationIcon(app.packageName)
                                        } catch (e: Exception) {}
                                    }

                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (iconDrawable != null) {
                                            Image(painter = rememberDrawablePainter(iconDrawable), contentDescription = null, modifier = Modifier.size(40.dp))
                                        } else {
                                            Box(modifier = Modifier.size(40.dp).background(Color.Gray, RoundedCornerShape(8.dp)))
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(app.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                            val reasonText = if (app.reasons.isNotEmpty()) "${app.packageName} • ${app.reasons}" else app.packageName
                                            Text(reasonText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "* Scheduled blocks and calendar events sync automatically. You can manually pull-to-refresh on the Study schedules list, or wait for the instant real-time sync.\n\n* When a block session ends, blocked apps will be unblocked automatically upon your next screen unlock.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun StatusRow(title: String, isActive: Boolean, iconRes: Int, activeText: String = "Active") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(id = iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                text = if (isActive) activeText else "Inactive",
                color = if (isActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
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
}
