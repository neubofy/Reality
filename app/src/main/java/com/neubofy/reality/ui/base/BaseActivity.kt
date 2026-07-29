package com.neubofy.reality.ui.base

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.reality.R
import com.neubofy.reality.utils.ThemeManager

/**
 * Base Activity to apply global personalization (Theming, Backgrounds, Glassmorphism).
 */
open class BaseActivity : AppCompatActivity() {

    private var loadingDialog: Dialog? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        val scale = ThemeManager.getFontSizeScale(newBase)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.fontScale = scale
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        ThemeManager.applyAccentTheme(this)
        
        // Elite Motion: Apply Activity Transitions
        applyEliteTransitions()
        
        super.onCreate(savedInstanceState)
    }

    private fun applyEliteTransitions() {
        val preset = ThemeManager.getMotionPreset(this)
        // Note: For real "Stiff" vs "Bouncy", we'd use custom XML animations with different interpolators.
        // For now, we'll map them to system/app animations or overridePendingTransition.
        // In a real premium app, we'd have Anim/motion_stiff_enter.xml etc.
    }

    private val identityUpdateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent == null || isFinishing || isDestroyed) return
            
            hideLoading()
            onIdentityUpdated()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-enforce Edge-to-Edge flags (Fixes nav bar glitch on back nav)
        ThemeManager.enforceEdgeToEdge(this)
        
        // Apply Global Background (Color + Pattern)
        ThemeManager.applyAppBackground(window.decorView)

        // Recursively apply all personalization
        // Force Insets Update to prevent spacing glitches
        val rootView = findViewById<View>(android.R.id.content)
        if (rootView != null) {
            ThemeManager.applyGlobalPersonalization(rootView)
            androidx.core.view.ViewCompat.requestApplyInsets(rootView)
        }

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
            identityUpdateReceiver,
            android.content.IntentFilter("com.neubofy.reality.IDENTITY_UPDATED")
        )
    }

    override fun onPause() {
        super.onPause()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(identityUpdateReceiver)
    }

    open fun onIdentityUpdated() {
        // Can be overridden by subclasses to update UI dynamically
    }
    
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyGlobalPersonalization()
    }
    
    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyGlobalPersonalization()
    }
    
    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyGlobalPersonalization()
    }

    private fun applyGlobalPersonalization() {
        val rootView = findViewById<View>(android.R.id.content)
        if (rootView != null) {
            ThemeManager.applyGlobalPersonalization(rootView)
        }
    }

    private val loadingHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hideLoadingRunnable = Runnable { hideLoading() }

    fun showLoading(message: String = "Loading...") {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            
            loadingHandler.removeCallbacks(hideLoadingRunnable)
            
            if (loadingDialog == null) {
                loadingDialog = Dialog(this@BaseActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(R.layout.fragment_auth_loading)
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    setCancelable(false)
                }
            }
            
            loadingDialog?.findViewById<TextView>(R.id.tv_loading_message)?.text = message
            
            if (loadingDialog?.isShowing == false) {
                loadingDialog?.show()
            }
            
            // Safety timeout: 5 seconds max
            loadingHandler.postDelayed(hideLoadingRunnable, 5000)
        }
    }

    fun hideLoading() {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            loadingHandler.removeCallbacks(hideLoadingRunnable)
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
            }
        }
    }
}
