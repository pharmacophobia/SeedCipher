package com.seedcipher.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seedcipher.app.ui.theme.*
import com.seedcipher.app.utils.SecurityPreferences

@Composable
fun PinLockScreen(
    isFirstTimeSetup: Boolean,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleNumKey(num: String) {
        errorMessage = null
        if (isFirstTimeSetup) {
            if (!isConfirmStep) {
                if (pinInput.length < 4) {
                    pinInput += num
                    if (pinInput.length == 4) {
                        isConfirmStep = true
                    }
                }
            } else {
                if (confirmPinInput.length < 4) {
                    confirmPinInput += num
                    if (confirmPinInput.length == 4) {
                        if (pinInput == confirmPinInput) {
                            SecurityPreferences.savePin(context, pinInput)
                            Toast.makeText(context, "PIN Created Successfully!", Toast.LENGTH_SHORT).show()
                            onUnlockSuccess()
                        } else {
                            errorMessage = "PINs do not match. Try again."
                            confirmPinInput = ""
                            pinInput = ""
                            isConfirmStep = false
                        }
                    }
                }
            }
        } else {
            if (pinInput.length < 4) {
                pinInput += num
                if (pinInput.length == 4) {
                    if (SecurityPreferences.verifyPin(context, pinInput)) {
                        onUnlockSuccess()
                    } else {
                        errorMessage = "Incorrect PIN. Please try again."
                        pinInput = ""
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        errorMessage = null
        if (isFirstTimeSetup && isConfirmStep) {
            if (confirmPinInput.isNotEmpty()) {
                confirmPinInput = confirmPinInput.dropLast(1)
            } else {
                isConfirmStep = false
                pinInput = pinInput.dropLast(1)
            }
        } else {
            if (pinInput.isNotEmpty()) {
                pinInput = pinInput.dropLast(1)
            }
        }
    }

    val currentLength = if (isFirstTimeSetup && isConfirmStep) confirmPinInput.length else pinInput.length
    val titleText = when {
        isFirstTimeSetup && !isConfirmStep -> "Setup Secure Access"
        isFirstTimeSetup && isConfirmStep -> "Confirm Security PIN"
        else -> "Welcome Back"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D47A1)), // Banking Blue
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // App Bank Logo
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(90.dp)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = titleText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )

            Text(
                text = if (isFirstTimeSetup) "Create a 4-digit PIN for your secure vault" else "Please enter your PIN to continue",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 4 Pin Dot Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < currentLength
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) Color.White else Color.Transparent)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

            // Error Message
            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = Color(0xFFFFCDD2), // Soft Red
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 24.dp)
                )
            } ?: Spacer(modifier = Modifier.height(40.dp))

            Spacer(modifier = Modifier.height(30.dp))

            // Numeric Keypad (3x4 grid)
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                keypad.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Fingerprint,
                                                contentDescription = "Biometric",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                                "DEL" -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clickable { handleBackspace() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Backspace,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .size(72.dp)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                            .clickable { handleNumKey(key) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            TextButton(onClick = { /* Help action */ }) {
                Text("Need help signing in?", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}
