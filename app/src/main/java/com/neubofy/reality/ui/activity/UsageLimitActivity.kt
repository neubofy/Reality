package com.neubofy.reality.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.reality.ui.base.BaseActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import com.neubofy.reality.databinding.ActivityUsageLimitBinding
import com.neubofy.reality.databinding.ItemAppUsageBinding
import com.neubofy.reality.utils.SavedPreferencesLoader
import com.neubofy.reality.utils.StrictLockUtils
import java.util.concurrent.TimeUnit

class UsageLimitActivity : BaseActivity() {

    private lateinit var binding: ActivityUsageLimitBinding
    private lateinit var savedPreferencesLoader: SavedPreferencesLoader
    private var usageData = com.neubofy.reality.Constants.UsageLimitData()
    private var appList = mutableListOf<AppUsageItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsageLimitBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        savedPreferencesLoader = SavedPreferencesLoader(this)
        usageData = savedPreferencesLoader.getUsageLimitData()
        
        
        setupToolbar()
        setupGlobalLimitUI()
        
        binding.recyclerAppUsage.layoutManager = LinearLayoutManager(this)
        
        loadAppsAndUsage()
    }
    


    private fun setupToolbar() {
        val toolbar = binding.includeHeader.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Usage Limits"
        toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupGlobalLimitUI() {
        binding.switchEnable.isChecked = usageData.isEnabled
        binding.containerGlobalLimit.visibility = if (usageData.isEnabled) View.VISIBLE else View.GONE
        
        val hours = usageData.limitInMinutes / 60
        val mins = usageData.limitInMinutes % 60
        binding.tvCurrentLimit.text = "${hours}h ${mins}m"

        binding.npHours.minValue = 0
        binding.npHours.maxValue = 16
        binding.npHours.value = hours.coerceIn(0, 16)

        binding.npMinutes.minValue = 0
        binding.npMinutes.maxValue = 59
        binding.npMinutes.value = mins.coerceIn(0, 59)
        
        binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && !StrictLockUtils.isModificationAllowed(this)) {
                Toast.makeText(this, "Disabling limit locked by Strict Mode", Toast.LENGTH_SHORT).show()
                binding.switchEnable.isChecked = true
                return@setOnCheckedChangeListener
            }
            usageData.isEnabled = isChecked
            binding.containerGlobalLimit.visibility = if (isChecked) View.VISIBLE else View.GONE
            saveData()
        }
        
        val onValueChangeListener = android.widget.NumberPicker.OnValueChangeListener { _, _, _ ->
            val value = (binding.npHours.value * 60) + binding.npMinutes.value
            if (!StrictLockUtils.isModificationAllowed(this)) {
                if (value > usageData.limitInMinutes) {
                    Toast.makeText(this, "Increase limit locked by Strict Mode.", Toast.LENGTH_SHORT).show()
                    binding.npHours.value = usageData.limitInMinutes / 60
                    binding.npMinutes.value = usageData.limitInMinutes % 60
                    return@OnValueChangeListener
                }
            }
            usageData.limitInMinutes = value
            val h = usageData.limitInMinutes / 60
            val m = usageData.limitInMinutes % 60
            binding.tvCurrentLimit.text = "${h}h ${m}m"
            saveData()
        }

        binding.npHours.setOnValueChangedListener(onValueChangeListener)
        binding.npMinutes.setOnValueChangedListener(onValueChangeListener)
    }

    private val scope = kotlinx.coroutines.MainScope()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun loadAppsAndUsage() {
        scope.launch(Dispatchers.Default) {
             val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
             val profiles = launcherApps.profiles
             val tempMap = hashMapOf<String, AppUsageItem>()
             
             // FETCH FRESH USAGE STATS
             val freshUsageMap = com.neubofy.reality.utils.UsageUtils.getUsageSinceMidnight(this@UsageLimitActivity)
             
             // 1. Get Installed Apps
             for (profile in profiles) {
                 val apps = launcherApps.getActivityList(null, profile).map { it.applicationInfo }
                 apps.forEach { info ->
                     if (info.packageName != packageName) {
                         val label = info.loadLabel(packageManager).toString()
                         // Get usage from fresh map
                         val usageMs = freshUsageMap.getOrDefault(info.packageName, 0L)
                         val limitMins = usageData.appLimits.getOrDefault(info.packageName, 0)
                         tempMap[info.packageName] = AppUsageItem(info.packageName, label, info, usageMs, limitMins)
                     }
                 }
             }
             
             // 2. Others (apps with usage but maybe not in launcher list)
             freshUsageMap.keys.forEach { pkg ->
                 if (!tempMap.containsKey(pkg)) {
                      try {
                          val info = packageManager.getApplicationInfo(pkg, 0)
                          val label = info.loadLabel(packageManager).toString()
                          val usageMs = freshUsageMap[pkg] ?: 0L
                          val limitMins = usageData.appLimits.getOrDefault(pkg, 0)
                          tempMap[pkg] = AppUsageItem(pkg, label, info, usageMs, limitMins)
                      } catch (e: Exception) {}
                 }
             }
             
             appList = tempMap.values.sortedByDescending { it.usageMs }.toMutableList()
             
             val totalMs = appList.sumOf { it.usageMs }
             val totalHours = TimeUnit.MILLISECONDS.toHours(totalMs)
             val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60
             
             withContext(Dispatchers.Main) {
                 binding.tvTotalTime.text = "${totalHours}h ${totalMinutes}m"
                 binding.recyclerAppUsage.adapter = AppUsageAdapter(appList)
             }
        }
    }
    
    private fun saveData() {
        savedPreferencesLoader.saveUsageLimitData(usageData)
    }
    
    private fun showSetLimitDialog(item: AppUsageItem, position: Int) {
         // Programmatic layout for simplicity
         val layout = android.widget.LinearLayout(this).apply {
             orientation = android.widget.LinearLayout.VERTICAL
             setPadding(50, 40, 50, 10)
         }
         
         val tvLimit = TextView(this).apply {
             text = "Limit: ${item.limitMins / 60}h ${item.limitMins % 60}m"
             textSize = 18f
             setTextColor(android.graphics.Color.WHITE)
             gravity = android.view.Gravity.CENTER
         }
         
         val pickerLayout = android.widget.LinearLayout(this).apply {
             orientation = android.widget.LinearLayout.HORIZONTAL
             gravity = android.view.Gravity.CENTER
         }
         
         val npHours = android.widget.NumberPicker(this).apply {
             minValue = 0
             maxValue = 16
             value = item.limitMins / 60
         }

         val tvHours = TextView(this).apply {
             text = " h "
             textSize = 18f
             setTextColor(android.graphics.Color.WHITE)
         }

         val npMinutes = android.widget.NumberPicker(this).apply {
             minValue = 0
             maxValue = 59
             value = item.limitMins % 60
         }

         val tvMinutes = TextView(this).apply {
             text = " m "
             textSize = 18f
             setTextColor(android.graphics.Color.WHITE)
         }

         pickerLayout.addView(npHours)
         pickerLayout.addView(tvHours)
         pickerLayout.addView(npMinutes)
         pickerLayout.addView(tvMinutes)

         val onValueChangeListener = android.widget.NumberPicker.OnValueChangeListener { _, _, _ ->
             tvLimit.text = "Limit: ${npHours.value}h ${npMinutes.value}m"
         }

         npHours.setOnValueChangedListener(onValueChangeListener)
         npMinutes.setOnValueChangedListener(onValueChangeListener)
         
         layout.addView(tvLimit)
         layout.addView(pickerLayout)
         
         MaterialAlertDialogBuilder(this)
             .setTitle("Set Limit for ${item.appName}")
             .setView(layout)
             .setPositiveButton("Save") { _, _ ->
                 val newLimit = (npHours.value * 60) + npMinutes.value
                 if (newLimit < item.limitMins && item.limitMins > 0 && !StrictLockUtils.isModificationAllowed(this)) {
                      // Increasing restriction (lower limit) is ALLOWED.
                 }
                 
                 if (newLimit > item.limitMins && item.limitMins > 0 && !StrictLockUtils.isModificationAllowed(this)) {
                     Toast.makeText(this, "Extending limit locked by Strict Mode.", Toast.LENGTH_SHORT).show()
                     return@setPositiveButton
                 }
                 if (newLimit == 0 && item.limitMins > 0 && !StrictLockUtils.isModificationAllowed(this)) {
                      Toast.makeText(this, "Removing limit locked by Strict Mode.", Toast.LENGTH_SHORT).show()
                      return@setPositiveButton
                 }
                 
                 item.limitMins = newLimit
                 if (newLimit > 0) {
                     usageData.appLimits[item.packageName] = newLimit
                 } else {
                     usageData.appLimits.remove(item.packageName)
                 }
                 saveData()
                 binding.recyclerAppUsage.adapter?.notifyItemChanged(position)
             }
             .setNegativeButton("Cancel", null)
             .setNeutralButton("Remove Limit") { _, _ ->
                 if (item.limitMins > 0 && !StrictLockUtils.isModificationAllowed(this)) {
                      Toast.makeText(this, "Removing limit locked by Strict Mode.", Toast.LENGTH_SHORT).show()
                      return@setNeutralButton
                 }
                 item.limitMins = 0
                 usageData.appLimits.remove(item.packageName)
                 saveData()
                 binding.recyclerAppUsage.adapter?.notifyItemChanged(position)
             }
             .show()
    }

    inner class AppUsageAdapter(private val list: List<AppUsageItem>) : RecyclerView.Adapter<AppUsageAdapter.VH>() {
        inner class VH(val binding: ItemAppUsageBinding) : RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(ItemAppUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.binding.tvAppName.text = item.appName
            
            val usedMins = TimeUnit.MILLISECONDS.toMinutes(item.usageMs)
            if (item.limitMins > 0) {
                 holder.binding.tvUsageTime.text = "${usedMins}m / ${item.limitMins}m"
                 holder.binding.progressBar.max = item.limitMins
                 holder.binding.progressBar.progress = usedMins.toInt()
                 if (usedMins >= item.limitMins) {
                     holder.binding.tvUsageTime.setTextColor(getColor(android.R.color.holo_red_light))
                 } else {
                     holder.binding.tvUsageTime.setTextColor(getColor(android.R.color.white))
                 }
            } else {
                 holder.binding.tvUsageTime.text = "${usedMins}m"
                 holder.binding.progressBar.max = 60 
                 holder.binding.progressBar.progress = usedMins.toInt()
                 holder.binding.tvUsageTime.setTextColor(getColor(android.R.color.white))
            }
            
            holder.binding.ivIcon.setImageDrawable(null)
            scope.launch(Dispatchers.IO) {
                try {
                    val icon = item.appInfo.loadIcon(packageManager)
                    withContext(Dispatchers.Main) { holder.binding.ivIcon.setImageDrawable(icon) }
                } catch(e: Exception) {}
            }
            
            holder.itemView.setOnClickListener {
                showSetLimitDialog(item, position)
            }
        }
        
        override fun getItemCount() = list.size
    }
    
    data class AppUsageItem(
        val packageName: String,
        val appName: String,
        val appInfo: ApplicationInfo,
        val usageMs: Long,
        var limitMins: Int
    )
}
