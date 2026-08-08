package com.neubofy.reality.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neubofy.reality.R
import com.neubofy.reality.ui.base.BaseActivity
import com.neubofy.reality.ui.theme.RealityTheme
import kotlinx.coroutines.delay

/**
 * First-launch activity that introduces the app and collects user name.
 * After user continues, navigates to OnboardingActivity for permission setup.
 */
class SecurityIntroActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealityTheme {
                SecurityIntroScreen(
                    onGetStarted = { userName ->
                        if (userName.isEmpty()) {
                            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                        } else {
                            // Save Name & Mark Intro as Shown
                            val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("user_name", userName)
                                .putBoolean("intro_shown", true)
                                .apply()

                            // Navigate directly to MainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SecurityIntroScreen(onGetStarted: (String) -> Unit) {
    var userName by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                    initialOffsetY = { -50 }, animationSpec = tween(500)
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_security_shield),
                    contentDescription = "Security Shield",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 32.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = 200)
                )
            ) {
                Text(
                    text = "Welcome to Reality",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = 400)
                )
            ) {
                Text(
                    text = "Reality is designed with a zero-compromise approach to your privacy and security. The app is 99.9% source-available and stores everything securely on your device.",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .alpha(0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Glassmorphism-like background for features
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 600)) + slideInVertically(
                    initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = 600)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    FeatureItem("Secure Sync: Private connection for Sync & Pro")
                    Spacer(modifier = Modifier.height(8.dp))
                    FeatureItem("Zero Data Collection: Your data never leaves")
                    Spacer(modifier = Modifier.height(8.dp))
                    FeatureItem("unbreakable App Locks")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 800)) + slideInVertically(
                    initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = 800)
                )
            ) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter your name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 1000))
            ) {
                Text(
                    text = "Disclaimer: Reality requires several system permissions to enforce blocker modes. Since 99.9% of our code is source-available, you can verify that your personal data is handled securely.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 1000)) + slideInVertically(
                    initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = 1000)
                )
            ) {
                Button(
                    onClick = { onGetStarted(userName.trim()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "• $text",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp
        )
    }
}
