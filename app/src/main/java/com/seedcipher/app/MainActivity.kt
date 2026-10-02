package com.seedcipher.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seedcipher.app.ui.components.PinLockScreen
import com.seedcipher.app.ui.components.SmsMessengerScreen
import com.seedcipher.app.ui.theme.*
import com.seedcipher.app.utils.SecurityPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureVaultTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    var isUnlocked by remember { mutableStateOf(false) }
    var isPinSet by remember { mutableStateOf(SecurityPreferences.isPinSet(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    val roleManagerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    fun requestDefaultSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                roleManagerLauncher.launch(intent)
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(context) != context.packageName) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                }
                roleManagerLauncher.launch(intent)
            }
        }
    }

    fun requestSmsPermissions() {
        requestDefaultSmsRole()
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_WAP_PUSH,
                Manifest.permission.READ_CONTACTS
            )
        )
    }

    // PIN Lock Screen
    if (!isUnlocked) {
        PinLockScreen(
            isFirstTimeSetup = !isPinSet,
            onUnlockSuccess = {
                isUnlocked = true
                isPinSet = true
                requestDefaultSmsRole()
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SecureVault", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("PRIVATE ACCESS", fontSize = 10.sp, color = TextSecondary, letterSpacing = 1.sp)
                        }

                        // Default SMS App Request Button
                        IconButton(onClick = { requestDefaultSmsRole() }) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = "Set Default SMS App",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Lock App Button
                        IconButton(onClick = { isUnlocked = false }) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Lock App",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SmsMessengerScreen(
                onRequestPermissions = { requestSmsPermissions() }
            )
        }
    }
}
