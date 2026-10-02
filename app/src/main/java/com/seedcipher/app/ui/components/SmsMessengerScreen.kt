package com.seedcipher.app.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seedcipher.app.scrambler.SeedGenerator
import com.seedcipher.app.scrambler.SeedScramblerEngine
import com.seedcipher.app.sms.SmsMessageItem
import com.seedcipher.app.sms.SmsReceiver
import com.seedcipher.app.sms.SmsRepository
import com.seedcipher.app.sms.SmsThread
import com.seedcipher.app.ui.theme.*
import com.seedcipher.app.utils.SecurityPreferences
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import com.seedcipher.app.stego.DecoyConversationData
import com.seedcipher.app.stego.EmojiStegoEngine
import com.seedcipher.app.stego.PhotoStegoEngine
import java.text.SimpleDateFormat
import java.util.*

enum class EncryptionMode(val label: String, val code: String) {
    CRYPTO_VAULT("AES-256 Vault", "CV1"),
    EMOJI_CIPHER("Emoji Cipher", "EM1"),
    SEED_PERMUTE("Seed Permute", "SP1"),
    STEGO_HIDE("Steganography", "ST1")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsMessengerScreen(
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(SmsRepository.hasSmsPermissions(context)) }
    var seedKey by remember { mutableStateOf(SecurityPreferences.getSavedSeedKey(context)) }
    var selectedMode by remember { mutableStateOf(EncryptionMode.CRYPTO_VAULT) }
    var threads by remember { mutableStateOf<List<SmsThread>>(emptyList()) }
    var selectedThread by remember { mutableStateOf<SmsThread?>(null) }
    var recipientNumber by remember { mutableStateOf("") }
    var composeText by remember { mutableStateOf("") }
    var isStegoCoverText by remember { mutableStateOf("Hey, hope you are doing well!") }
    var showInspectorDialog by remember { mutableStateOf(false) }
    var stegoLensActive by remember { mutableStateOf(false) }
    var showStegoComposerDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Reload function
    val reloadSms = {
        hasPermission = SmsRepository.hasSmsPermissions(context)
        val fetched = SmsRepository.fetchSmsThreads(context, seedKey)
        threads = fetched
        if (selectedThread != null) {
            val updated = fetched.find { it.address == selectedThread?.address }
            if (updated != null) {
                selectedThread = updated
            }
        }
    }

    // Effect for initial load and listening to incoming SMS
    DisposableEffect(seedKey) {
        reloadSms()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == SmsReceiver.ACTION_SMS_RECEIVED) {
                    reloadSms()
                }
            }
        }

        val filter = IntentFilter(SmsReceiver.ACTION_SMS_RECEIVED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // TOP CONTROL PANEL (Seed Key & Mode Config)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "SECURE VAULT ACCESS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { seedKey = SeedGenerator.generateSeedWord("phrase") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Random Key",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = seedKey,
                    onValueChange = {
                        seedKey = it
                        SecurityPreferences.saveSeedKey(context, it)
                    },
                    placeholder = { Text("Enter shared seed password...", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Encryption Mode Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EncryptionMode.values().forEach { mode ->
                        val isSelected = (selectedMode == mode)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = mode }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DarkBackground else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // PERMISSION REQUIRED BANNER IF NOT GRANTED
        if (!hasPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SMS Permission Required",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            "SecureVault needs SMS permissions to send and auto-decrypt messages.",
                            color = Color(0xFFFECACA),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Grant", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // MAIN CONTENT AREA: Thread List or Active Chat
        if (selectedThread == null) {
            // CONVERSATIONS / THREADS LIST VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // New Message Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = recipientNumber,
                        onValueChange = { recipientNumber = it },
                        placeholder = { Text("Enter recipient phone number...", color = TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (recipientNumber.isNotBlank()) {
                                val existing = threads.find { it.address.contains(recipientNumber) || recipientNumber.contains(it.address) }
                                if (existing != null) {
                                    selectedThread = existing
                                } else {
                                    val newThread = SmsThread(
                                        threadId = System.currentTimeMillis(),
                                        address = recipientNumber,
                                        contactName = recipientNumber,
                                        lastMessage = SmsMessageItem(
                                            id = 0, address = recipientNumber, body = "New conversation", timestamp = System.currentTimeMillis(), isOutgoing = false
                                        )
                                    )
                                    selectedThread = newThread
                                }
                            } else {
                                Toast.makeText(context, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = "Start Chat", tint = DarkBackground)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text("RECENT THREADS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { reloadSms() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Sync, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No SMS Conversations Found", color = TextSecondary, fontSize = 14.sp)
                            Text("Enter a phone number above or grant SMS access to start messaging.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(threads) { thread ->
                            SmsThreadCard(thread = thread, onClick = { selectedThread = thread })
                        }
                    }
                }
            }
        } else {
            // ACTIVE CHAT VIEW FOR SELECTED THREAD
            val currentThread = selectedThread!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // CHAT HEADER
                Surface(
                    color = CardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        IconButton(onClick = { selectedThread = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentThread.contactName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentThread.contactName,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = currentThread.address,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        // Stego Lens Mode Toggle Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (stegoLensActive) Color(0xFF064E3B) else Color(0xFF0F172A),
                            modifier = Modifier
                                .border(1.dp, if (stegoLensActive) Color(0xFF10B981) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { stegoLensActive = !stegoLensActive }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (stegoLensActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Stego Lens Toggle",
                                    tint = if (stegoLensActive) Color(0xFF34D399) else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (stegoLensActive) "STEGO LENS: ON" else "CAMOUFLAGE",
                                    fontSize = 9.sp,
                                    color = if (stegoLensActive) Color(0xFF34D399) else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(selectedMode.code, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // MESSAGES LIST
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(currentThread.messages) { message ->
                        ChatMessageBubble(message = message, seedKey = seedKey, stegoLensActive = stegoLensActive)
                    }
                }

                // COMPOSE BAR & DIRECT ENCRYPTED SEND
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (selectedMode == EncryptionMode.STEGO_HIDE) {
                            Text("Stego Cover Text:", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = isStegoCoverText,
                                onValueChange = { isStegoCoverText = it },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = composeText,
                                onValueChange = { composeText = it },
                                placeholder = { Text("Type message to encrypt & send...", color = TextSecondary, fontSize = 13.sp) },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { showStegoComposerDialog = true },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Stego Photo & Emoji Composer", tint = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { showInspectorDialog = true },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = "Scramble Tools", tint = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    if (composeText.isBlank()) return@Button
                                    if (!hasPermission) {
                                        onRequestPermissions()
                                        return@Button
                                    }

                                    // Encrypt message according to selected mode
                                    val scrambledPayload = when (selectedMode) {
                                        EncryptionMode.CRYPTO_VAULT -> SeedScramblerEngine.scrambleCryptoVault(composeText, seedKey)
                                        EncryptionMode.EMOJI_CIPHER -> SeedScramblerEngine.scrambleEmoji(composeText, seedKey)
                                        EncryptionMode.SEED_PERMUTE -> SeedScramblerEngine.scrambleSeedPermute(composeText, seedKey)
                                        EncryptionMode.STEGO_HIDE -> {
                                            val secret = SeedScramblerEngine.scrambleCryptoVault(composeText, seedKey)
                                            SeedScramblerEngine.hideInStego(isStegoCoverText, secret)
                                        }
                                    }

                                    val success = SmsRepository.sendSmsMessage(context, currentThread.address, scrambledPayload)
                                    if (success) {
                                        Toast.makeText(context, "Encrypted SMS Sent!", Toast.LENGTH_SHORT).show()
                                        // Optimistically add to UI list
                                        val newMsg = SmsMessageItem(
                                            id = System.currentTimeMillis(),
                                            address = currentThread.address,
                                            body = scrambledPayload,
                                            timestamp = System.currentTimeMillis(),
                                            isOutgoing = true,
                                            decryptedBody = composeText,
                                            isEncryptedPayload = true
                                        )
                                        selectedThread = currentThread.copy(
                                            messages = currentThread.messages + newMsg
                                        )
                                        composeText = ""
                                    } else {
                                        Toast.makeText(context, "Failed to send SMS. Check cellular signal & permissions.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send Encrypted SMS", tint = DarkBackground)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStegoComposerDialog) {
        StegoComposerDialog(
            currentSeed = seedKey,
            onDismiss = { showStegoComposerDialog = false },
            onSendPhotoStego = { caption, imagePath, secretText ->
                val currentThread = selectedThread ?: return@StegoComposerDialog
                val newMsg = SmsMessageItem(
                    id = System.currentTimeMillis(),
                    address = currentThread.address,
                    body = caption,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = true,
                    decryptedBody = secretText,
                    isEncryptedPayload = true,
                    hasPhotoStego = true,
                    imageUri = imagePath,
                    photoSecret = secretText,
                    stegoCoverText = caption
                )
                selectedThread = currentThread.copy(
                    messages = currentThread.messages + newMsg
                )
                if (hasPermission && currentThread.threadId != DecoyConversationData.DECOY_THREAD_ID) {
                    SmsRepository.sendSmsMessage(context, currentThread.address, caption)
                }
                Toast.makeText(context, "Concealed in Photo & Posted!", Toast.LENGTH_SHORT).show()
            },
            onSendEmojiStego = { coverTextWithEmojis, fullStegoText, secretText ->
                val currentThread = selectedThread ?: return@StegoComposerDialog
                val newMsg = SmsMessageItem(
                    id = System.currentTimeMillis(),
                    address = currentThread.address,
                    body = fullStegoText,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = true,
                    decryptedBody = secretText,
                    isEncryptedPayload = true,
                    hasEmojiStego = true,
                    emojiSecret = secretText,
                    stegoCoverText = coverTextWithEmojis
                )
                selectedThread = currentThread.copy(
                    messages = currentThread.messages + newMsg
                )
                if (hasPermission && currentThread.threadId != DecoyConversationData.DECOY_THREAD_ID) {
                    SmsRepository.sendSmsMessage(context, currentThread.address, fullStegoText)
                }
                Toast.makeText(context, "Concealed in Emojis & Sent!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showInspectorDialog) {
        ScrambleInspectorDialog(
            currentSeed = seedKey,
            onDismiss = { showInspectorDialog = false },
            onInsertPayload = { payload ->
                composeText = payload
            }
        )
    }
}

@Composable
fun SmsThreadCard(
    thread: SmsThread,
    onClick: () -> Unit
) {
    val dateStr = remember(thread.lastMessage.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(thread.lastMessage.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = thread.contactName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.contactName,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (thread.lastMessage.isEncryptedPayload) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = thread.lastMessage.decryptedBody ?: thread.lastMessage.body,
                        fontSize = 12.sp,
                        color = if (thread.lastMessage.isEncryptedPayload) MaterialTheme.colorScheme.primary else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: SmsMessageItem,
    seedKey: String,
    stegoLensActive: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (isOutgoing) Color(0xFF0F172A) else SurfaceDark
    val borderColor = if (isOutgoing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFF334155)

    // Local override state allowing tapping a bubble to reveal secret even if Stego Lens is OFF
    var individualReveal by remember { mutableStateOf(false) }
    val isRevealed = stegoLensActive || individualReveal

    // Resolve photo bitmap if present
    val photoBitmap = remember(message.imageUri) {
        message.imageUri?.let { path ->
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                null
            }
        }
    }

    // Photo secret resolution
    val resolvedPhotoSecret = remember(message.imageUri, photoBitmap, seedKey) {
        if (message.photoSecret != null) {
            message.photoSecret
        } else if (photoBitmap != null) {
            val rawSecret = PhotoStegoEngine.extractSecretFromBitmap(photoBitmap)
            if (rawSecret != null) {
                val (_, decrypted) = SmsRepository.processAutoDecryption(rawSecret, seedKey)
                decrypted ?: rawSecret
            } else null
        } else null
    }

    // Auto-decryption for text/emoji payloads
    val (isEncrypted, decryptedText) = remember(message.body, seedKey) {
        if (message.decryptedBody != null) {
            Pair(true, message.decryptedBody)
        } else {
            SmsRepository.processAutoDecryption(message.body, seedKey)
        }
    }

    val (hasEmojiStego, _) = remember(message.body) {
        EmojiStegoEngine.extractFromCoverText(message.body)
    }

    val displayCoverText = remember(message.body, message.stegoCoverText, hasEmojiStego) {
        when {
            message.stegoCoverText != null -> message.stegoCoverText
            hasEmojiStego -> EmojiStegoEngine.cleanVisibleText(message.body)
            else -> message.body
        }
    }

    val effectiveSecret = resolvedPhotoSecret ?: message.emojiSecret ?: decryptedText

    val timeStr = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 16.dp
            ),
            color = bubbleColor,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .border(
                    1.dp,
                    if (isRevealed && effectiveSecret != null) Color(0xFF10B981).copy(alpha = 0.6f) else borderColor,
                    RoundedCornerShape(16.dp)
                )
                .clickable {
                    if (effectiveSecret != null && !stegoLensActive) {
                        individualReveal = !individualReveal
                    }
                }
        ) {
            Column(modifier = Modifier.padding(10.dp)) {

                // 1. PHOTO ATTACHMENT DISPLAY
                if (photoBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 210.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .padding(bottom = 6.dp)
                    ) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = "Photo Attachment",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )

                        // Subtle LSB indicator when revealed
                        if (isRevealed && resolvedPhotoSecret != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF064E3B).copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("PHOTO LSB STEGO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }
                        }
                    }
                }

                // 2. REVEALED SECRET DRAWER (Visible when Lens is ON or bubble is tapped)
                if (isRevealed && effectiveSecret != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF022C22)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .border(1.dp, Color(0xFF059669), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (photoBitmap != null) Icons.Default.PhotoCamera else Icons.Default.EmojiEmotions,
                                    contentDescription = "Stego Secret",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (photoBitmap != null) "PHOTO LSB SECRET:" else "EMOJI STEGO SECRET:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(effectiveSecret))
                                        Toast.makeText(context, "Secret copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Secret", tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            SelectionContainer {
                                Text(
                                    text = effectiveSecret,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 3. COVER CONVERSATION TEXT
                // In Camouflage Mode (Default), this is all anyone sees! Completely ordinary conversation with emojis!
                SelectionContainer {
                    Text(
                        text = if (isRevealed && !hasEmojiStego && photoBitmap == null && isEncrypted && !message.body.startsWith("http")) {
                            message.body
                        } else {
                            displayCoverText
                        },
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ScrambleInspectorDialog(
    currentSeed: String,
    onDismiss: () -> Unit,
    onInsertPayload: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("crypto") }
    val clipboardManager = LocalClipboardManager.current
    var qrPayload by remember { mutableStateOf<String?>(null) }

    val scrambledOutput = remember(inputText, currentSeed, selectedMode) {
        if (inputText.isEmpty()) ""
        else when (selectedMode) {
            "emoji" -> SeedScramblerEngine.scrambleEmoji(inputText, currentSeed)
            "crypto" -> SeedScramblerEngine.scrambleCryptoVault(inputText, currentSeed)
            "stego" -> {
                val secret = SeedScramblerEngine.scrambleSeedPermute(inputText, currentSeed)
                SeedScramblerEngine.hideInStego("Hey! Meeting up for coffee tomorrow at 3pm?", secret)
            }
            else -> SeedScramblerEngine.scrambleSeedPermute(inputText, currentSeed)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scramble Inspector & Tools", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type text to test scramble cipher...", fontSize = 12.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )

                if (scrambledOutput.isNotEmpty()) {
                    Text("Scrambled Payload:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    SelectionContainer {
                        Text(
                            text = scrambledOutput,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (scrambledOutput.isNotEmpty()) {
                Button(
                    onClick = {
                        onInsertPayload(scrambledOutput)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Use in SMS", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )

    qrPayload?.let { payload ->
        QRCodeDialog(payload = payload, onDismiss = { qrPayload = null })
    }
}
