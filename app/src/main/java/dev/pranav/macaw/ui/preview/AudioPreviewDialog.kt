package dev.pranav.macaw.ui.preview

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.pranav.macaw.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPreviewDialog(audioFile: File, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playerState by PlaybackService.playerState.collectAsState(initial = PlaybackService.PlayerState())
    val isPlaying = playerState.isPlaying
    val currentPosition = playerState.position
    val duration = playerState.duration
    val currentFile = playerState.currentFilePath?.let { File(it) } ?: audioFile

    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLongPressed by remember { mutableStateOf(false) }

    LaunchedEffect(currentFile.absolutePath) {
        thumbnail = withContext(Dispatchers.IO) {
            extractAudioThumbnail(currentFile.absolutePath)
        }
    }

    DisposableEffect(audioFile.absolutePath) {
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY
            putExtra(PlaybackService.EXTRA_AUDIO_FILE_PATH, audioFile.absolutePath)
            putExtra(PlaybackService.EXTRA_AUDIO_TITLE, audioFile.nameWithoutExtension)
        }
        context.startService(intent)

        onDispose {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }

    val blurRadius by animateFloatAsState(
        targetValue = if (isLongPressed) 6f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "blurAnimation"
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .blur(blurRadius.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    currentFile.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AudioThumbnail(
                thumbnail = thumbnail,
                isLongPressed = isLongPressed,
                onLongPressChanged = { isLongPressed = it },
                modifier = Modifier
                    .size(200.dp)
                    .zIndex(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .blur(blurRadius.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlaybackSlider(
                    currentTime = currentPosition,
                    totalDuration = duration,
                    onSliderPositionChange = { value ->
                        val newPosition = (value * duration).toLong()
                        PlaybackService.seekTo(newPosition)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { PlaybackService.playPrevious() },
                        enabled = playerState.hasPrevious
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier
                                .height(32.dp)
                                .width(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            if (isPlaying) PlaybackService.pause() else PlaybackService.play()
                        },
                        enabled = duration > 0L
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = { PlaybackService.playNext() },
                        enabled = playerState.hasNext
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier
                                .height(32.dp)
                                .width(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioThumbnail(
    thumbnail: Bitmap?,
    isLongPressed: Boolean,
    onLongPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isLongPressed) 1.5f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scaleAnimation"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isLongPressed) 16f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "elevationAnimation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .scale(scale)
            .graphicsLayer {
                shadowElevation = elevation
            }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (isLongPressed) {
                        onLongPressChanged(false)
                    }
                },
                onLongClick = { onLongPressChanged(true) },
                onLongClickLabel = "Expand"
            ),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = "Audio thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }
    }
}

private fun extractAudioThumbnail(filePath: String): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val embeddedPicture = retriever.embeddedPicture
        retriever.release()

        embeddedPicture?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    } catch (_: Exception) {
        null
    }
}

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressUpdateJob: Job? = null

    private var playlist: List<File> = emptyList()
    private var currentPlaylistIndex = -1

    data class PlayerState(
        val isPlaying: Boolean = false,
        val position: Long = 0L,
        val duration: Long = 0L,
        val currentMediaItem: MediaItem? = null,
        val currentFilePath: String? = null,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false
    )

    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SEEK_TO = "ACTION_SEEK_TO"
        const val ACTION_SEEK_TO_PREVIOUS = "ACTION_SEEK_TO_PREVIOUS"
        const val ACTION_SEEK_TO_NEXT = "ACTION_SEEK_TO_NEXT"

        const val EXTRA_AUDIO_FILE_PATH = "AUDIO_FILE_PATH"
        const val EXTRA_AUDIO_TITLE = "AUDIO_TITLE"
        const val EXTRA_SEEK_POSITION = "SEEK_POSITION"

        private const val NOTIFICATION_CHANNEL_ID = "audio_playback_channel"
        private const val NOTIFICATION_ID = 100

        private val _playerState = MutableStateFlow(PlayerState())
        val playerState: StateFlow<PlayerState> = _playerState

        private var instance: PlaybackService? = null

        fun play() {
            instance?.player?.let { player ->
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
        }

        fun pause() = instance?.player?.pause()
        fun stop() = instance?.player?.stop()

        fun seekTo(position: Long) {
            instance?.player?.let { player ->
                val wasEnded = player.playbackState == Player.STATE_ENDED

                player.seekTo(position)

                if (wasEnded || !player.isPlaying) {
                    player.play()
                }

                instance?.updatePlayerState()
            }
        }

        fun seekToPrevious() = instance?.player?.seekToPrevious()
        fun seekToNext() = instance?.player?.seekToNext()

        fun playNext() = instance?.playNextAudioFile()
        fun playPrevious() = instance?.playPreviousAudioFile()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this

        player = ExoPlayer.Builder(this).build().apply {
            addListener(createPlayerListener())
        }

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(createPendingIntent())
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )

        createNotificationChannel()
    }

    private fun createPlayerListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlayerState()
            if (playbackState == Player.STATE_ENDED) {
                player?.pause()
                updatePlayerState()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayerState()
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePlayerState()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePlayerState()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_PLAY -> {
                intent.getStringExtra(EXTRA_AUDIO_FILE_PATH)?.let { path ->
                    val audioFile = File(path)
                    val title = intent.getStringExtra(EXTRA_AUDIO_TITLE) ?: "Unknown"

                    // Build playlist from directory
                    buildPlaylistFromDirectory(audioFile)

                    // Find the index of the current file in the playlist
                    currentPlaylistIndex = playlist.indexOfFirst { it.absolutePath == audioFile.absolutePath }

                    // Play the file
                    player?.apply {
                        setMediaItem(createMediaItem(path, title))
                        prepare()
                        playWhenReady = true
                    }

                    updatePlayerState()
                }
            }

            ACTION_PAUSE -> pause()
            ACTION_STOP -> stop()
            ACTION_SEEK_TO -> seekTo(intent.getLongExtra(EXTRA_SEEK_POSITION, 0L))
            ACTION_SEEK_TO_PREVIOUS -> playPreviousAudioFile()
            ACTION_SEEK_TO_NEXT -> playNextAudioFile()
        }

        return START_STICKY
    }

    private fun buildPlaylistFromDirectory(audioFile: File) {
        val directory = audioFile.parentFile

        if (directory != null && directory.exists() && directory.isDirectory) {
            playlist = directory.listFiles { file ->
                val extension = file.extension.lowercase(Locale.ROOT)
                file.isFile && (extension == "mp3" || extension == "wav" || extension == "ogg" || extension == "m4a" || extension == "aac")
            }?.sortedBy { it.name } ?: emptyList()
        } else {
            playlist = listOf(audioFile)
        }
    }

    fun playNextAudioFile() {
        if (playlist.isEmpty() || currentPlaylistIndex < 0) return

        val nextIndex = (currentPlaylistIndex + 1) % playlist.size
        playFileAtIndex(nextIndex)
    }

    fun playPreviousAudioFile() {
        if (playlist.isEmpty() || currentPlaylistIndex < 0) return

        val previousIndex = if (currentPlaylistIndex > 0) currentPlaylistIndex - 1 else playlist.size - 1
        playFileAtIndex(previousIndex)
    }

    private fun playFileAtIndex(index: Int) {
        if (index < 0 || index >= playlist.size) return

        val file = playlist[index]
        currentPlaylistIndex = index

        player?.apply {
            setMediaItem(createMediaItem(file.absolutePath, file.nameWithoutExtension))
            prepare()
            playWhenReady = true
        }

        updatePlayerState()
    }

    private fun createMediaItem(filePath: String, title: String): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setArtist("File Manager")
            .build()

        return MediaItem.Builder()
            .setUri(filePath.toUri())
            .setMediaId(filePath)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = scope.launch {
            while (isActive && player?.isPlaying == true) {
                updatePlayerState()
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
    }

    private fun updatePlayerState() {
        player?.let { currentPlayer ->
            _playerState.value = PlayerState(
                isPlaying = currentPlayer.isPlaying,
                position = currentPlayer.currentPosition.coerceAtLeast(0L),
                duration = if (currentPlayer.duration == androidx.media3.common.C.TIME_UNSET) 0L
                else currentPlayer.duration.coerceAtLeast(0L),
                currentMediaItem = currentPlayer.currentMediaItem,
                currentFilePath = currentPlayer.currentMediaItem?.mediaId,
                hasNext = playlist.size > 1,
                hasPrevious = playlist.size > 1
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Audio Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for audio playback control"
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (player?.isPlaying != true) {
            player?.pause()
            updatePlayerState()
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopProgressUpdates()
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        instance = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSlider(
    currentTime: Long,
    totalDuration: Long,
    onSliderPositionChange: (Float) -> Unit
) {
    var sliderPosition by remember(currentTime, totalDuration) {
        mutableFloatStateOf(if (totalDuration > 0) currentTime.toFloat() / totalDuration else 0f)
    }

    // Track if the user is currently dragging the slider
    var isDragging by remember { mutableStateOf(false) }

    // Calculate the time to display - use dragged position when dragging
    val displayTime = if (isDragging && totalDuration > 0)
        (sliderPosition * totalDuration).toLong()
    else
        currentTime

    val isEnabled = totalDuration > 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDuration(displayTime),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp)
        )
        SquigglySlider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                inactiveTrackColor = MaterialTheme.colorScheme.tertiaryContainer,
                activeTrackColor = MaterialTheme.colorScheme.tertiary,
            ),
            squigglesSpec = SquigglySlider.SquigglesSpec(
                strokeWidth = 4.dp,
                wavelength = 24.dp,
                amplitude = 4.dp,
            ),
            value = sliderPosition.coerceIn(0f, 1f),
            onValueChange = { newValue ->
                isDragging = true
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                if (isEnabled && totalDuration > 0) {
                    onSliderPositionChange(sliderPosition)
                    // If we reached the end and are seeking back, ensure we start playing
                    if (currentTime >= totalDuration - 500) {
                        PlaybackService.play()
                    }
                }
                isDragging = false
            },
            valueRange = 0f..1f,
            enabled = isEnabled
        )
        Text(
            text = formatDuration(totalDuration),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val nonNegativeMillis = millis.coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(nonNegativeMillis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(nonNegativeMillis) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
