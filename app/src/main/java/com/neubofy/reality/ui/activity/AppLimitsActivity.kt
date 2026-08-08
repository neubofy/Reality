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
import com.neubofy.reality.R
import com.neubofy.reality.databinding.ActivitySingleFragmentBinding
import com.neubofy.reality.ui.base.BaseActivity
import com.neubofy.reality.ui.fragments.AppLimitsFragment
import com.neubofy.reality.ui.theme.RealityTheme

class AppLimitsActivity : BaseActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RealityTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("App Limits") },
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
                            val binding = ActivitySingleFragmentBinding.inflate(inflater, parent, attachToParent)

                            binding.topAppBar.visibility = android.view.View.GONE

                            val fragmentManager = (this@AppLimitsActivity as androidx.fragment.app.FragmentActivity).supportFragmentManager
                            if (fragmentManager.findFragmentById(R.id.fragment_container) == null) {
                                fragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, AppLimitsFragment())
                                    .commit()
                            }

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
}
