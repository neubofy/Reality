package com.neubofy.reality.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.neubofy.reality.databinding.ActivityUnifiedBlocklistBinding
import com.neubofy.reality.ui.base.BaseActivity
import com.neubofy.reality.ui.fragments.BlocklistAppsFragment
import com.neubofy.reality.ui.fragments.BlocklistWebsitesFragment
import com.neubofy.reality.ui.theme.RealityTheme

class UnifiedBlocklistActivity : BaseActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            RealityTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Manage Blocklist") },
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
                        AndroidViewBinding(factory = { inflater, parent, attachToParent ->
                            val binding = ActivityUnifiedBlocklistBinding.inflate(inflater, parent, attachToParent)

                            binding.includeHeader.root.visibility = android.view.View.GONE

                            val adapter = ViewPagerAdapter(this@UnifiedBlocklistActivity)
                            binding.viewPager.adapter = adapter

                            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                                tab.text = when(position) {
                                    0 -> "Apps"
                                    1 -> "Websites"
                                    else -> null
                                }
                            }.attach()

                            binding
                        })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.neubofy.reality.utils.PermissionHelper.checkAndPromptForCore(this)
    }
    
    inner class ViewPagerAdapter(activity: androidx.fragment.app.FragmentActivity) : androidx.viewpager2.adapter.FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return when(position) {
                0 -> BlocklistAppsFragment()
                1 -> BlocklistWebsitesFragment()
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}
