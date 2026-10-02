package com.seedcipher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seedcipher.app.scrambler.SeedGenerator
import com.seedcipher.app.scrambler.SeedScramblerEngine
import com.seedcipher.app.ui.theme.*
import android.content.Intent

@Composable
fun ScrambleScreen(
    onOpenQR: (String) -> Unit,
    onOpenStego: (String) -> Unit
) {
    var mode by remember { mutableStateOf("permute") } // permute, emoji, crypto, stego
    var message by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var seed by remember { mutableStateOf(com.seedcipher.app.utils.SecurityPreferences.getSavedSeedKey(context)) }
    var scrambledResult by remember { mutableStateOf("") }
    var isScrambling by remember { mutableStateOf(false) }

    val seedStrength = SeedGenerator.evaluateSeedStrength(seed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, SurfaceDark, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ModeChip("Shuffle", Icons.Default.Shuffle, mode == "permute") { mode = "permute" }
            ModeChip("Emoji", Icons.Default.SentimentSatisfied, mode == "emoji") { mode = "emoji" }
            ModeChip("AES-256", Icons.Default.Lock, mode == "crypto") { mode = "crypto" }
            ModeChip("Stego", Icons.Default.VisibilityOff, mode == "stego") { mode = "stego" }
        }

        // Original Message Box
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Original Secret Message", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Text("${message.length} chars", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Type secret message here...", fontSize = 13.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = SurfaceDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Sample Quick Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = { message = "Meet me at midnight behind the library." },
                        label = { Text("Midnight sample", fontSize = 10.sp) }
                    )
                    SuggestionChip(
                        onClick = { message = "Launch Code: Alpha-992-Delta" },
                        label = { Text("Launch code sample", fontSize = 10.sp) }
                    )
                }
            }
        }

        // Seed Word Input & Strength Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Seed Word / Key", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    TextButton(onClick = { seed = SeedGenerator.generateSeedWord("phrase") }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto Seed", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedTextField(
                    value = seed,
                    onValueChange = {
                        seed = it
                        com.seedcipher.app.utils.SecurityPreferences.saveSeedKey(context, it)
                    },
                    placeholder = { Text("e.g. dragon-cipher-42", fontSize = 13.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = IndigoAccent,
                        unfocusedBorderColor = SurfaceDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Strength Meter
                if (seed.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Strength:", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "${seedStrength.label} (${seedStrength.entropyBits} bits)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(seedStrength.colorHex)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { seedStrength.score / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = Color(seedStrength.colorHex),
                            trackColor = DarkBackground
                        )
                    }
                }
            }
        }

        // Action Scramble Button
        Button(
            onClick = {
                if (message.isNotEmpty()) {
                    scrambledResult = when (mode) {
                        "emoji" -> SeedScramblerEngine.scrambleEmoji(message, seed)
                        "crypto" -> SeedScramblerEngine.scrambleCryptoVault(message, seed)
                        "stego" -> {
                            val inner = SeedScramblerEngine.scrambleSeedPermute(message, seed)
                            SeedScramblerEngine.hideInStego("Hey! Meeting up for coffee tomorrow at 3pm?", inner)
                        }
                        else -> SeedScramblerEngine.scrambleSeedPermute(message, seed)
                    }
                }
            },
            enabled = message.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = DarkBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scramble Message", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkBackground)
        }

        // Scrambled Payload Result Card
        if (scrambledResult.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scrambled Output Payload", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(mode.uppercase(), fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }

                    SelectionContainer {
                        Text(
                            text = scrambledResult,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { clipboardManager.setText(AnnotatedString(scrambledResult)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, scrambledResult)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Scrambled Payload"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { onOpenQR(scrambledResult) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) DarkBackground else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) DarkBackground else TextSecondary
            )
        }
    }
}
