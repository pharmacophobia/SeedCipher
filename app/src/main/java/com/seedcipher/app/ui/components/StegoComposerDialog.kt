package com.seedcipher.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seedcipher.app.scrambler.SeedGenerator
import com.seedcipher.app.scrambler.SeedScramblerEngine
import com.seedcipher.app.stego.EmojiStegoEngine
import com.seedcipher.app.stego.PhotoStegoEngine
import com.seedcipher.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StegoComposerDialog(
    currentSeed: String,
    onDismiss: () -> Unit,
    onSendPhotoStego: (caption: String, imagePath: String, secretText: String) -> Unit,
    onSendEmojiStego: (coverTextWithEmojis: String, fullStegoText: String, secretText: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Photo Stego, 1 = Emoji Stego

    // Secret input shared across tabs
    var secretMessage by remember { mutableStateOf("") }

    // Photo Stego state
    var selectedScene by remember { mutableStateOf(PhotoStegoEngine.DecoyScene.COFFEE_CUP) }
    var photoCaption by remember { mutableStateOf(PhotoStegoEngine.DecoyScene.COFFEE_CUP.coverCaption) }
    var customBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var customImageUri by remember { mutableStateOf<Uri?>(null) }

    // Active preview bitmap
    val activeBitmap = remember(selectedScene, customBitmap) {
        customBitmap ?: PhotoStegoEngine.generateDecoyBitmap(selectedScene)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = PhotoStegoEngine.loadBitmapFromUri(context, uri)
            if (bmp != null) {
                customBitmap = bmp
                customImageUri = uri
                photoCaption = "Look at this photo from today! 📸"
            } else {
                Toast.makeText(context, "Could not load selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Emoji Stego state
    var selectedTopicIndex by remember { mutableStateOf(0) }
    var coverText by remember { mutableStateOf(EmojiStegoEngine.DECOY_TOPICS[0].sampleMessages[0]) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp)),
            color = CardBackground,
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Steganography Covert Composer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Plausible deniability via photos & natural emojis",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Carrier Medium Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceDark,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Photo Stego", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Emoji & Dialogue", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secret Message Input (Always visible)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SECRET MESSAGE TO CONCEAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    secretMessage = SeedGenerator.generateSeedWords(12).joinToString(" ")
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+ 12-Word Seed", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        OutlinedTextField(
                            value = secretMessage,
                            onValueChange = { secretMessage = it },
                            placeholder = { Text("Enter mnemonic seed words, passwords, or confidential text...", fontSize = 12.sp, color = TextSecondary) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable tab content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedTab == 0) {
                        // ----------------------------------------------------
                        // TAB 0: PHOTO STEGANOGRAPHY
                        // ----------------------------------------------------
                        Text("1. CHOOSE COVER PHOTO CARRIER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                        // Presets Row
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PhotoStegoEngine.DecoyScene.values().forEach { scene ->
                                val isSelected = (customBitmap == null && selectedScene == scene)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        customBitmap = null
                                        selectedScene = scene
                                        photoCaption = scene.coverCaption
                                    },
                                    label = { Text(scene.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = DarkBackground
                                    )
                                )
                            }

                            FilterChip(
                                selected = customBitmap != null,
                                onClick = { galleryLauncher.launch("image/*") },
                                label = { Text("📁 Pick Gallery Photo", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = DarkBackground
                                )
                            )
                        }

                        // Preview Image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .background(SurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = activeBitmap.asImageBitmap(),
                                contentDescription = "Decoy Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "LSB Carrier Ready",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Caption editor
                        Text("2. NATURAL COVER CAPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        OutlinedTextField(
                            value = photoCaption,
                            onValueChange = { photoCaption = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        // ----------------------------------------------------
                        // TAB 1: EMOJI & NATURAL DIALOGUE STEGANOGRAPHY
                        // ----------------------------------------------------
                        Text("1. SELECT COVER TOPIC (PLAUSIBLE DENIABILITY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EmojiStegoEngine.DECOY_TOPICS.forEachIndexed { index, topic ->
                                val isSelected = (selectedTopicIndex == index)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTopicIndex = index
                                        coverText = topic.sampleMessages.first()
                                    },
                                    label = { Text("${topic.icon} ${topic.category}", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = DarkBackground
                                    )
                                )
                            }
                        }

                        // Sample phrases in current topic
                        Text("2. QUICK SELECT NATURAL PHRASE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        val activeTopic = EmojiStegoEngine.DECOY_TOPICS[selectedTopicIndex]
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            activeTopic.sampleMessages.forEach { msg ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (coverText == msg) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else SurfaceDark,
                                    border = BorderStroke(1.dp, if (coverText == msg) MaterialTheme.colorScheme.primary else Color(0xFF334155)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { coverText = msg }
                                ) {
                                    Text(
                                        text = msg,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        Text("3. EDIT COVER TEXT WITH EMOJIS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        OutlinedTextField(
                            value = coverText,
                            onValueChange = { coverText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Quick Emoji insert buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("☕️", "🍕", "✨", "👍", "🌮", "🏖️", "💻", "🎉").forEach { emoji ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SurfaceDark,
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { coverText = "$coverText $emoji" }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(emoji, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (secretMessage.isBlank()) {
                                Toast.makeText(context, "Please enter a secret message to conceal", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Encrypt secret with active seed key
                            val encryptedSecret = SeedScramblerEngine.scrambleCryptoVault(
                                secretMessage.trim(),
                                currentSeed.ifEmpty { "default_seed" }
                            )

                            if (selectedTab == 0) {
                                // Embed in Photo
                                try {
                                    val stegoBitmap = PhotoStegoEngine.embedSecretInBitmap(activeBitmap, encryptedSecret)
                                    val savedFile = PhotoStegoEngine.saveBitmapToCache(context, stegoBitmap, "user_stego")
                                    onSendPhotoStego(photoCaption, savedFile.absolutePath, secretMessage.trim())
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error embedding in photo: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                // Embed in Emoji Cover
                                try {
                                    val fullStegoText = EmojiStegoEngine.embedInCoverText(coverText, encryptedSecret)
                                    onSendEmojiStego(coverText, fullStegoText, secretMessage.trim())
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error embedding in emojis: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (selectedTab == 0) "Conceal in Photo & Send" else "Conceal in Emojis & Send",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
