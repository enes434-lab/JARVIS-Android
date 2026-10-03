package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private var onResult: ((String) -> Unit)? = null

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) listen() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { tts.language = Locale.GERMAN }
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        setContent {
            var transcript by remember { mutableStateOf("Bereit") }
            var response by remember { mutableStateOf("System online. Wie kann ich helfen?") }
            var listening by remember { mutableStateOf(false) }

            JarvisTheme {
                JarvisScreen(
                    transcript = transcript,
                    response = response,
                    listening = listening,
                    onListen = {
                        onResult = { text ->
                            transcript = text
                            val answer = executeCommand(text)
                            response = answer
                            speak(answer)
                        }
                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED) listen()
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        listening = true
                    },
                    onStop = { recognizer?.stopListening(); listening = false },
                    onCommand = { cmd ->
                        transcript = cmd
                        response = executeCommand(cmd)
                        speak(response)
                    }
                )
            }
        }
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { onResult?.invoke(it) }
            }
            override fun onError(error: Int) { onResult?.invoke("Spracherkennung nicht verfügbar.") }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(r: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, b: Bundle?) {}
        })
        recognizer?.startListening(intent)
    }

    private fun executeCommand(raw: String): String {
        val c = raw.lowercase(Locale.GERMAN).trim()
        return when {
            c.contains("youtube") -> { openUrl("https://www.youtube.com"); "YouTube wird geöffnet." }
            c.contains("google") -> { openUrl("https://www.google.com"); "Google wird geöffnet." }
            c.contains("browser") -> { openUrl("https://www.google.com"); "Browser wird geöffnet." }
            c.contains("wlan") || c.contains("wifi") -> { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)); "WLAN-Einstellungen geöffnet."; }
            c.contains("bluetooth") -> { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); "Bluetooth-Einstellungen geöffnet."; }
            c.contains("einstellungen") -> { startActivity(Intent(Settings.ACTION_SETTINGS)); "Einstellungen geöffnet."; }
            c.contains("anrufen") || c.contains("telefon") -> { startActivity(Intent(Intent.ACTION_DIAL)); "Telefon-App geöffnet."; }
            c.contains("karte") || c.contains("navigation") -> { openUrl("https://maps.google.com"); "Karten werden geöffnet." }
            c.contains("wecker") -> {
                startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS")
                })
                "Wecker-App geöffnet."
            }
            c.contains("hallo") || c.contains("jarvis") -> "Guten Tag. JARVIS ist bereit."
            else -> "Befehl erkannt: $raw. Dafür ist in dieser Version noch kein Modul eingerichtet."
        }
    }

    private fun openUrl(url: String) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (i.resolveActivity(packageManager) != null) startActivity(i)
    }

    private fun speak(text: String) { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }

    override fun onDestroy() {
        recognizer?.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}

@Composable
fun JarvisScreen(
    transcript: String, response: String, listening: Boolean,
    onListen: () -> Unit, onStop: () -> Unit, onCommand: (String) -> Unit
) {
    var command by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF05070D)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            Text("J.A.R.V.I.S.", color = Color(0xFF62D9FF), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("PERSONAL COMMAND CENTER", color = Color(0xFF6D8290), fontSize = 11.sp)
            Spacer(Modifier.height(30.dp))

            Box(
                modifier = Modifier.size(220.dp).background(
                    if (listening) Color(0xFF0E3342) else Color(0xFF0A1820), CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { if (listening) onStop() else onListen() },
                    modifier = Modifier.size(150.dp)
                ) {
                    Icon(
                        if (listening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Sprachsteuerung",
                        tint = Color(0xFF62D9FF),
                        modifier = Modifier.size(76.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(if (listening) "ICH HÖRE ZU..." else "BEREIT", color = Color(0xFF62D9FF), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B111A)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("SPRACHBEFEHL", color = Color(0xFF6D8290), fontSize = 11.sp)
                    Text(transcript, color = Color.White, fontSize = 17.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("JARVIS", color = Color(0xFF6D8290), fontSize = 11.sp)
                    Text(response, color = Color(0xFFB8C7D1), fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = command, onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Befehl eingeben") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { if (command.isNotBlank()) { onCommand(command); command = "" } }) {
                        Icon(Icons.Default.Send, contentDescription = "Senden")
                    }
                }
            )
            Spacer(Modifier.height(14.dp))
            Text("Beispiele: „Öffne YouTube“ • „WLAN“ • „Stelle einen Wecker“ • „Öffne Navigation“",
                color = Color(0xFF6D8290), fontSize = 12.sp)
        }
    }
}

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF62D9FF),
            background = Color(0xFF05070D),
            surface = Color(0xFF0B111A)
        ),
        content = content
    )
}
