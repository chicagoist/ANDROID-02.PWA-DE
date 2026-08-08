/*
 * Just German — учебный проект
 * Copyright (c) 2026 chicagoist
 *
 * SPDX-License-Identifier: LicenseRef-proprietary
 *
 * Аудио, учебник и метод Assimil принадлежат правообладателю Assimil SAS
 * (Франция, assimil.com). Распространение этих материалов запрещено.
 * См. файл NOTICE.
 */

package com.chicagoist.justgerman.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chicagoist.justgerman.data.model.DialogLine
import com.chicagoist.justgerman.data.repository.LessonRepository
import com.chicagoist.justgerman.data.repository.MediaStore
import com.chicagoist.justgerman.data.repository.ProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.chicagoist.justgerman.ui.theme.FlagBlack
import com.chicagoist.justgerman.ui.theme.Gold
import com.chicagoist.justgerman.ui.theme.QuizCorrect
import com.chicagoist.justgerman.ui.theme.Zinc800
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { LessonRepository(context) }
    val lesson = remember { repository.getLesson(lessonId) }

    if (lesson == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Урок не найден")
        }
        return
    }

    // Resolve media: imported files (from resources.zip) take priority,
    // bundled APK assets are the fallback. Hoisted to screen scope so the
    // audio keeps playing even when the player card scrolls out of view.
    val mediaStore = remember { MediaStore(context) }
    val audioUri = remember(lesson.audioPath) { mediaStore.resolveAudioUri(lesson.audioPath) }
    val player = remember(audioUri) {
        audioUri?.let { uri ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
            }
        }
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    // Lesson completion progress (DataStore)
    val progressRepository = remember { ProgressRepository(context) }
    val completedLessons by progressRepository.completedLessons
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val isCompleted = lesson.id in completedLessons
    val scope = rememberCoroutineScope()

    // Single TTS engine for the whole lesson screen.
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        var disposed = false
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS && !disposed) {
                // Prefer German; fall back to the default voice if unavailable.
                val result = engine?.setLanguage(Locale.GERMAN)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    engine?.setLanguage(Locale.getDefault())
                }
                tts.value = engine
            }
        }
        onDispose {
            disposed = true
            engine?.stop()
            engine?.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Урок ${lesson.id}")
                        Text(
                            lesson.title.ifBlank { lesson.topics.joinToString(" · ") },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio player
            item {
                val p = player
                if (p != null) {
                    LessonAudioPlayer(player = p)
                } else {
                    MediaMissingCard()
                }
            }

            // PDF textbook button: opens the bundled or imported textbook
            // in the user's preferred external PDF reader via
            // Intent.ACTION_VIEW + FileProvider. We deliberately rely on the
            // platform PdfRenderer-free path because Android 13 Go devices
            // (moto e13, Unisoc) fail to produce bitmaps through it, leaving
            // a per-page "Не удалось загрузить" error string.
            item {
                TextbookButton(mediaStore = mediaStore)
            }

            // Dialog
            item {
                SectionCard(title = "Диалог урока") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            lesson.dialog.forEachIndexed { index, line ->
                            DialogLineRow(
                                line = line,
                                index = index,
                                tts = tts.value
                            )
                        }
                    }
                }
            }

            // Vocabulary
            item {
                SectionCard(title = "Лексика урока") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lesson.vocabulary.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    item.german,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                // Russian sits at the right edge to mirror
                                // the desktop PWA's vocabulary card.
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        stripAutoMarker(item.russian),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Grammar
            if (lesson.grammar.isNotEmpty()) {
                item {
                    SectionCard(title = "Грамматика") {
                        Text(
                            lesson.grammar,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Essence
            if (lesson.essence.isNotEmpty()) {
                item {
                    SectionCard(title = "Суть урока") {
                        Text(
                            lesson.essence,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Difficulties
            if (lesson.difficulties.isNotEmpty()) {
                item {
                    SectionCard(title = "Скрытые сложности") {
                        BulletList(items = lesson.difficulties)
                    }
                }
            }

            // Common mistakes
            if (lesson.mistakes.isNotEmpty()) {
                item {
                    SectionCard(title = "Типичные ошибки") {
                        BulletList(items = lesson.mistakes)
                    }
                }
            }

            // Tips
            if (lesson.tips.isNotEmpty()) {
                item {
                    SectionCard(title = "Методические советы") {
                        BulletList(items = lesson.tips)
                    }
                }
            }

            // Mark lesson as completed (like the PWA)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            progressRepository.markCompleted(lesson.id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) Zinc800 else Gold,
                        contentColor = if (isCompleted) QuizCorrect else FlagBlack
                    )
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (isCompleted) "Урок пройден" else "Завершить урок",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold
                )
                Text(
                    item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Strips the ` [auto]` provenance marker that tools/translate-builtin.js appends
 * to machine-translated strings (Google Translate gtx_). The marker is preserved
 * in `lessons.json` for audit and easy grep-ing, but never reaches the user.
 */
private fun stripAutoMarker(s: String): String = s.removeSuffix(" [auto]")

@Composable
fun DialogLineRow(line: DialogLine, index: Int, tts: TextToSpeech?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Speaker button (left of the line, like the PWA)
        IconButton(
            onClick = {
                tts?.let { engine ->
                    engine.stop()
                    engine.speak(line.german, TextToSpeech.QUEUE_FLUSH, null, "dialog_$index")
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = "Озвучить",
                tint = Gold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                line.german,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "[${line.pronunciation}]",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )            // Always render Russian under each German line. The toggle and
            // per-line "машинный перевод" label were removed in favour of
            // always-on translation; the "↳ перевод уточняется" placeholder
            // stays for any future lesson whose dialog is still missing
            // Russian strings.
            if (line.russian.isNotBlank()) {
                Text(
                    stripAutoMarker(line.russian),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "↳ перевод уточняется",
                    style = MaterialTheme.typography.labelMedium,
                    color = Gold,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun LessonAudioPlayer(player: ExoPlayer) {
    // Initialize from the player so the card stays consistent after the
    // LazyColumn disposes and recomposes the item (listener only fires on
    // state *transitions*, so it would otherwise show stale play/0:00).
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var speed by remember { mutableFloatStateOf(1f) }
    var volume by remember { mutableFloatStateOf(1f) }
    var isSeeking by remember { mutableStateOf(false) }
    // Surface ExoPlayer prepare()/decode errors as visible text instead of
    // leaving the card stuck on "0:00 / 0:00" forever. A previous version had
    // no error listener, so the only symptom was a frozen progress bar.
    var playbackError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                    // A successful prepare clears any previous error so the
                    // card doesn't keep showing a stale alert after the user
                    // retries or switches lessons.
                    playbackError = null
                }
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    player.seekTo(0)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // errorCodeName is stable across ExoPlayer versions (e.g.
                // ERROR_CODE_IO_FILE_NOT_FOUND, ERROR_CODE_DECODER_INIT_FAILED).
                // Surface a short, single-line alert inside the card.
                playbackError = "Аудио не запустилось · ${error.errorCodeName}" +
                    (error.message?.let { " · $it" } ?: "")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    // Poll position while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isSeeking) {
                currentPosition = player.currentPosition
            }
            delay(200)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Аудио урока",
                style = MaterialTheme.typography.titleSmall,
                color = Gold
            )

            // Surface decoder/IO errors as an inline alert. Cleared by the
            // next STATE_READY transition (see listener above). Keeps the
            // audio card intact — only prepends a single error row.
            playbackError?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Progress bar + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatTime(if (isSeeking) currentPosition else player.currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = (if (isSeeking) currentPosition else player.currentPosition)
                        .toFloat()
                        .coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                    onValueChange = {
                        isSeeking = true
                        currentPosition = it.toLong()
                    },
                    onValueChangeFinished = {
                        player.seekTo(currentPosition)
                        isSeeking = false
                        currentPosition = player.currentPosition
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Gold,
                        activeTrackColor = Gold,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
                Text(
                    formatTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L))
                }) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Назад 10 секунд",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Gold,
                        contentColor = FlagBlack
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Слушать",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = {
                    player.seekTo((player.currentPosition + 10_000).coerceAtMost(duration))
                }) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Вперёд 10 секунд",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Speed control
                var showSpeedMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { showSpeedMenu = true }) {
                        Text(
                            "${formatSpeed(speed)}x",
                            style = MaterialTheme.typography.titleMedium,
                            color = Gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${formatSpeed(option)}x",
                                        color = if (option == speed) Gold else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    speed = option
                                    player.playbackParameters = PlaybackParameters(option)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                // Volume slider
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        player.volume = it
                    },
                    modifier = Modifier.width(96.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Gold,
                        activeTrackColor = Gold,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

/**
 * Opens [MediaStore.resolvePdfFile] in the user's preferred external PDF
 * viewer through an `Intent.ACTION_VIEW` + FileProvider URI grant.
 *
 * Why external rather than `Intent.ACTION_VIEW`:
 *   - Works on every Android version from 4.0.
 *   - The framework PdfRenderer produces empty bitmaps (silently, no
 *     exception thrown) on a few low-RAM SoCs (Unisoc / Android 13 Go).
 *   - Lets the user pick the reader they trust and keep their last-read
 *     page between launches.
 *
 * The resolve step runs on [Dispatchers.IO] because copying the bundled PDF
 * out of compressed APK assets on first launch takes ~200 ms.
 */
@Composable
fun TextbookButton(mediaStore: MediaStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var errorText by remember { mutableStateOf<String?>(null) }

    Button(
        onClick = {
            errorText = null
            scope.launch {
                val pdfFile = withContext(Dispatchers.IO) {
                    mediaStore.resolvePdfFile()
                }
                if (pdfFile == null) {
                    errorText = "Медиафайлы не найдены. Импортируйте resources.zip на главном экране"
                    return@launch
                }
                try {
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pdfFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    errorText = "Нет приложения для просмотра PDF. Установите любой PDF-ридер из Play Market"
                } catch (e: Exception) {
                    errorText = "Не удалось открыть учебник"
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = FlagBlack
        )
    ) {
        Text(
            "Открыть учебник (PDF)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }

    errorText?.let { msg ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            msg,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun MediaMissingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Аудио урока",
                style = MaterialTheme.typography.titleSmall,
                color = Gold
            )
            Text(
                "Медиафайлы не найдены. Импортируйте resources.zip на главном экране, чтобы включить аудио и учебник.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = Gold
            )
            content()
        }
    }
}

private fun formatTime(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        speed.toInt().toString()
    } else {
        speed.toString().trimEnd('0')
    }
}

