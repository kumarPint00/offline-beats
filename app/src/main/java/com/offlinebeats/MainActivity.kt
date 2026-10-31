package com.offlinebeats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinebeats.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val receivedLink = mutableStateOf("")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receivedLink.value = sharedUrl(intent)
        setContent { OfflineBeatsScreen(receivedLink.value) }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receivedLink.value = sharedUrl(intent)
    }
    private fun sharedUrl(intent: Intent?): String {
        val raw = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        } else ""
        return Regex("""https://[^\s]+""").find(raw)?.value
            ?.trimEnd(')', ']', ',', '.', ';').orEmpty()
    }
}

@Composable
private fun OfflineBeatsScreen(incomingUrl: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog = remember { CommonsCatalog() }
    val downloads = remember { AudioDownloads(context) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf(emptyList<CommonsAudio>()) }
    var rows by remember { mutableStateOf(downloads.history()) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(incomingUrl) {
        if (incomingUrl.isNotEmpty()) {
            link = incomingUrl
            selected = 1
            agreed = false
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF9EDCFF),
        surface = Color(0xFF232734),
        background = Color(0xFF11131B)
    )) {
        Column(Modifier.fillMaxSize().background(Color(0xFF11131B))) {
            Column(Modifier.padding(18.dp)) {
                Text("OfflineBeats", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text("Download songs. Play them in Samsung Music.",
                    color = Color(0xFFB5BACB))
            }
            val tabs = listOf("Discover", "Direct link", "Downloads")
            TabRow(selectedTabIndex = selected) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = selected == i, onClick = {
                        selected = i
                        if (i == 2) rows = downloads.history()
                    }, text = { Text(title) })
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                }
                when (selected) {
                    0 -> {
                        Text("Free-licensed catalog", style = MaterialTheme.typography.titleLarge)
                        Text("Search Wikimedia Commons. Review each song's creator and license.",
                            color = Color(0xFFB5BACB))
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("Search songs, artists or instruments") })
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            scope.launch {
                                loading = true
                                message = ""
                                try {
                                    found = withContext(Dispatchers.IO) { catalog.search(search) }
                                    if (found.isEmpty()) message = "No downloadable results found."
                                } catch (e: Exception) {
                                    message = e.message ?: "Search failed"
                                } finally { loading = false }
                            }
                        }, enabled = !loading && search.trim().length >= 2) {
                            Text(if (loading) "Searching…" else "Search")
                        }
                        Spacer(Modifier.height(10.dp))
                        found.forEach { song ->
                            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(song.name, fontWeight = FontWeight.SemiBold)
                                    Text(song.creator, color = Color(0xFFBEC7D9))
                                    Text(song.license, color = Color(0xFF9EDCFF))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { openWeb(context, song.sourceUrl) }) {
                                            Text("Source / license")
                                        }
                                        Button(onClick = {
                                            try {
                                                downloads.saveCommons(song)
                                                rows = downloads.history()
                                                message = "Added to downloads: " + song.name
                                            } catch (e: Exception) {
                                                message = e.message ?: "Download failed"
                                            }
                                        }) { Text("Save audio") }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        Text("Download from a link",
                            style = MaterialTheme.typography.titleLarge)
                        Text("Only direct HTTPS audio files are supported. Paste a link or share it here from another app.",
                            color = Color(0xFFB5BACB))
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = link, onValueChange = {
                            link = it.trim()
                            agreed = false
                        }, label = { Text("https://…/track.mp3") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Spacer(Modifier.height(12.dp))
                        when (LinkRules.classify(link)) {
                            LinkKind.DIRECT_AUDIO -> {
                                Row {
                                    Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                                    Text("I own this recording or have permission to download it.",
                                        modifier = Modifier.padding(top = 10.dp))
                                }
                                Button(enabled = agreed, onClick = {
                                    try {
                                        downloads.saveDirect(link, agreed)
                                        rows = downloads.history()
                                        message = "Download queued. Check Downloads."
                                        selected = 2
                                    } catch (e: Exception) {
                                        message = e.message ?: "Could not start download"
                                    }
                                }) { Text("Download audio") }
                            }
                            LinkKind.YOUTUBE -> {
                                Text("YouTube links cannot be extracted into an MP3 here. Use the official YouTube download option where available.",
                                    color = Color(0xFFFFC68B))
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { openWeb(context, link) }) {
                                    Text("Open in YouTube")
                                }
                            }
                            LinkKind.WEB_PAGE -> {
                                Text("This is a web page, not a direct audio file. Find its authorized file download link.",
                                    color = Color(0xFFFFC68B))
                                TextButton(onClick = { openWeb(context, link) }) {
                                    Text("Open source")
                                }
                            }
                            LinkKind.INVALID -> Text("Enter a valid HTTPS URL.",
                                color = Color(0xFFFFC68B))
                            LinkKind.EMPTY -> Text("Supports MP3, M4A, AAC, FLAC, OGG, OPUS, WAV.")
                        }
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Saved downloads", style = MaterialTheme.typography.titleLarge)
                            TextButton(onClick = { rows = downloads.history() }) { Text("Refresh") }
                        }
                        Text("Stored in Music/OfflineBeats. Samsung Music handles playback.",
                            color = Color(0xFFB5BACB))
                        Spacer(Modifier.height(10.dp))
                        if (rows.isEmpty()) Text("No songs queued yet.")
                        rows.forEach { entry ->
                            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(entry.name, fontWeight = FontWeight.SemiBold)
                                    Text(downloads.status(entry.id),
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(entry.credit + " • " + entry.license,
                                        color = Color(0xFFBEC7D9))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { openWeb(context, entry.source) }) {
                                            Text("Source")
                                        }
                                        if (entry.licenseUrl.isNotBlank()) {
                                            TextButton(onClick = { openWeb(context, entry.licenseUrl) }) {
                                                Text("License")
                                            }
                                        }
                                        if (downloads.status(entry.id) == "Saved") {
                                            Button(onClick = {
                                                try { downloads.openWithAudioApp(entry.id) }
                                                catch (e: Exception) {
                                                    message = e.message ?: "Cannot open file"
                                                }
                                            }) { Text("Open song") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("No ads • No account • No music player • No stream-ripping",
                    color = Color(0xFF9AA5B7))
            }
        }
    }
}

private fun openWeb(context: Context, raw: String) {
    if (LinkRules.classify(raw) == LinkKind.INVALID ||
        LinkRules.classify(raw) == LinkKind.EMPTY) return
    try { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(raw))) }
    catch (_: Exception) { /* No available browser; ignore safely. */ }
}
