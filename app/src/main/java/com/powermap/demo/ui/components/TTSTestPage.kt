package com.powermap.demo.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermap.demo.theme.PrimaryColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSTestPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var ttsStatus by remember { mutableStateOf("Initializing...") }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var textInput by remember { mutableStateOf("เลี้ยวซ้ายเข้าสู่ถนนเพชรบุรี") }
    var textInput2 by remember { mutableStateOf("Turn left onto Phetchaburi Road") }

    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        
        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsStatus = "Ready (Google TTS)"
            } else {
                ttsStatus = "Google TTS Failed, trying default... ($status)"
                ttsInstance = TextToSpeech(context) { fallbackStatus ->
                    if (fallbackStatus == TextToSpeech.SUCCESS) {
                        ttsStatus = "Ready (Default TTS)"
                    } else {
                        ttsStatus = "ERROR All Engines Failed: $fallbackStatus"
                    }
                }
                tts = ttsInstance
            }
        }
        
        ttsInstance = TextToSpeech(context, initListener, "com.google.android.tts")
        tts = ttsInstance

        onDispose {
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Box(modifier = Modifier.widthIn(max = 420.dp).fillMaxHeight().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("TTS Diagnostic Test", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Divider(color = Color(0xFFF1F5F9))

            Column(modifier = Modifier.padding(20.dp)) {
                // Status Box
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ttsStatus.contains("ERROR") || ttsStatus.contains("missing")) Color(0xFFFFE4E6) else Color(0xFFE0F2FE)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Engine Status:", fontWeight = FontWeight.Bold)
                        Text(ttsStatus, color = if (ttsStatus.contains("ERROR")) Color.Red else PrimaryColor)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Default Voice Engine:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(tts?.defaultEngine ?: "Unknown", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Thai Test
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Thai Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val result = tts?.setLanguage(Locale("th", "TH"))
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            ttsStatus = "THAI ERROR: Missing or unsupported (Code: $result)"
                        } else {
                            ttsStatus = "Speaking Thai (Code: $result)"
                            tts?.speak(textInput, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text("Speak Thai")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // English Test
                OutlinedTextField(
                    value = textInput2,
                    onValueChange = { textInput2 = it },
                    label = { Text("English Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val result = tts?.setLanguage(Locale.US)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            ttsStatus = "EN ERROR: Missing or unsupported (Code: $result)"
                        } else {
                            ttsStatus = "Speaking English (Code: $result)"
                            tts?.speak(textInput2, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Speak English")
                }
            }
        }
    }
}
}
