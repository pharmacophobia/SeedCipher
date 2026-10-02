package com.seedcipher.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seedcipher.app.scrambler.SeedScramblerEngine
import com.seedcipher.app.ui.theme.*

@Composable
fun UnscrambleScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var scrambledInput by remember { mutableStateOf("") }
    var seed by remember { mutableStateOf(com.seedcipher.app.utils.SecurityPreferences.getSavedSeedKey(context)) }
    var unscrambledOutput by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var hidePayload by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scrambled Input Card
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
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scrambled Payload / Stego Text", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    TextButton(onClick = {
                        val clip = clipboardManager.getText()
                        if (clip != null) scrambledInput = clip.text
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedTextField(
                    value = scrambledInput,
                    onValueChange = { scrambledInput = it },
                    placeholder = { Text("Paste payload (SP1:..., EM1:..., CV1:... or stego text) here...", fontSize = 13.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
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
            }
        }

        // Seed Word Input Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Seed Word (Required for Decryption)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                OutlinedTextField(
                    value = seed,
                    onValueChange = {
                        seed = it
                        com.seedcipher.app.utils.SecurityPreferences.saveSeedKey(context, it)
                    },
                    placeholder = { Text("Enter matching seed word...", fontSize = 13.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
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
            }
        }

        // Action Unscramble Button
        Button(
            onClick = {
                if (scrambledInput.isNotEmpty()) {
                    val (isStego, extracted) = SeedScramblerEngine.extractFromStego(scrambledInput.trim())
                    val payload = if (isStego) extracted else scrambledInput.trim()

                    val res = when {
                        payload.startsWith("EM1:") -> SeedScramblerEngine.unscrambleEmoji(payload, seed)
                        payload.startsWith("CV1:") -> SeedScramblerEngine.unscrambleCryptoVault(payload, seed)
                        else -> SeedScramblerEngine.unscrambleSeedPermute(payload, seed)
                    }

                    if (res.contains("❌") || res.contains("⚠️")) {
                        isError = true
                        isSuccess = false
                        unscrambledOutput = res
                    } else {
                        isError = false
                        isSuccess = true
                        unscrambledOutput = res
                    }
                }
            },
            enabled = scrambledInput.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unscramble Message", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        }

        // Result Output Card
        if (unscrambledOutput.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isError) RoseError else EmeraldSuccess
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isError) "Unscramble Failed" else "Original Message Decoded",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isError) RoseError else EmeraldSuccess
                        )

                        if (isSuccess) {
                            IconButton(onClick = { hidePayload = !hidePayload }) {
                                Icon(
                                    if (hidePayload) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    SelectionContainer {
                        Text(
                            text = if (hidePayload) "••••••••••••••••••••••••••••" else unscrambledOutput,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        )
                    }

                    if (isSuccess) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = { clipboardManager.setText(AnnotatedString(unscrambledOutput)) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Original Text", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
