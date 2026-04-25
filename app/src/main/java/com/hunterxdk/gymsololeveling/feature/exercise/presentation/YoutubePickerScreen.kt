package com.hunterxdk.gymsololeveling.feature.exercise.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubePickerScreen(
    currentVideoId: String? = null,
    onVideoSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    var videoIdInput by remember { mutableStateOf(currentVideoId ?: "") }
    var previewVideoId by remember { mutableStateOf(currentVideoId) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Exercise Video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Paste a YouTube video ID or full URL to preview the exercise demonstration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = videoIdInput,
                    onValueChange = { videoIdInput = it },
                    label = { Text("YouTube Video ID or URL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("e.g. dQw4w9WgXcQ") },
                )
                Button(onClick = {
                    val parsed = parseYoutubeId(videoIdInput)
                    if (parsed != null) previewVideoId = parsed
                }) {
                    Text("Preview")
                }
            }

            previewVideoId?.let { videoId ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                ) {
                    AndroidView(
                        factory = { ctx ->
                            YouTubePlayerView(ctx).also { playerView ->
                                lifecycleOwner.lifecycle.addObserver(playerView)
                                playerView.addYouTubePlayerListener(
                                    object : AbstractYouTubePlayerListener() {
                                        override fun onReady(youTubePlayer: YouTubePlayer) {
                                            youTubePlayer.cueVideo(videoId, 0f)
                                        }
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val parsed = parseYoutubeId(videoIdInput)
                    if (parsed != null) onVideoSelected(parsed)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = videoIdInput.isNotBlank(),
            ) {
                Text("Use This Video")
            }

            if (currentVideoId != null) {
                OutlinedButton(
                    onClick = { onVideoSelected("") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Remove Video")
                }
            }
        }
    }
}

private fun parseYoutubeId(input: String): String? {
    if (input.isBlank()) return null
    val urlPattern = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([A-Za-z0-9_-]{11})")
    val match = urlPattern.find(input)
    if (match != null) return match.groupValues[1]
    if (input.length == 11 && input.matches(Regex("[A-Za-z0-9_-]+"))) return input
    return null
}
