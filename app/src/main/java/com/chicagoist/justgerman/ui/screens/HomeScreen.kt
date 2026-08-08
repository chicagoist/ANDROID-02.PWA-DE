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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chicagoist.justgerman.R
import com.chicagoist.justgerman.data.repository.LessonRepository
import com.chicagoist.justgerman.data.repository.MediaStore
import com.chicagoist.justgerman.data.repository.ProgressRepository
import com.chicagoist.justgerman.ui.theme.Gold
import com.chicagoist.justgerman.ui.theme.Zinc800
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLesson: (Int) -> Unit,
    onNavigateToWeek: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaStore = remember { MediaStore(context) }

    // Lesson completion progress (DataStore)
    val progressRepository = remember { ProgressRepository(context) }
    val completedLessons by progressRepository.completedLessons
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val repository = remember { LessonRepository(context) }
    val lessons = remember { repository.getLessons() }
    val totalLessons = lessons.size
    // Total weeks is derived from the data so the home screen stays correct if
    // lessons.json ever grows or shrinks. Currently 15 (weeks 1-14 × 7 + week
    // 15 × 2 = 100 lessons). The previous "3 month" UI hid weeks 2-4, 6-8
    // and 10-15 even though WeekScreen could render them.
    val totalWeeks = remember(lessons) { lessons.maxOfOrNull { it.week } ?: 1 }
    val weeks = remember(lessons, completedLessons, totalWeeks) {
        val byWeek = lessons.groupBy { it.week }
        (1..totalWeeks).map { weekId ->
            val inWeek = byWeek[weekId].orEmpty()
            WeekEntry(
                weekId = weekId,
                total = inWeek.size,
                completed = inWeek.count { it.id in completedLessons }
            )
        }
    }

    var importState by remember { mutableStateOf(ImportState.Idle) }
    var importedCount by remember { mutableIntStateOf(mediaStore.importedMediaCount()) }
    val hasBundled = remember { mediaStore.hasBundledMedia() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                importState = ImportState.Importing
                val count = withContext(Dispatchers.IO) {
                    try {
                        mediaStore.importResourcesZip(uri)
                    } catch (e: Exception) {
                        -1
                    }
                }
                if (count >= 0) {
                    importedCount = withContext(Dispatchers.IO) {
                        mediaStore.importedMediaCount()
                    }
                    importState = ImportState.Done
                } else {
                    importState = ImportState.Error
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Just German",
                        style = MaterialTheme.typography.headlineSmall
                    )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Intro
            item {
                // Russian pluralization: 1 неделя / 2-4 недели / 5-20 недель.
                // Source of truth always matches the actual weeks rendered below.
                val weekWord = when {
                    totalWeeks % 10 == 1 && totalWeeks % 100 != 11 -> "неделя"
                    totalWeeks % 10 in 2..4 && (totalWeeks % 100 !in 12..14) -> "недели"
                    else -> "недель"
                }
                Text(
                    "$totalWeeks $weekWord интенсива",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Немецкий A2 → B1 по методу Ассимиля",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Progress banner (like the PWA)
            item {
                ProgressBanner(
                    completedCount = completedLessons.size,
                    totalCount = totalLessons
                )
            }

            // One card per course week (matches lessons.json: 15 weeks × ~7 lessons)
            items(weeks) { week ->
                WeekCard(
                    weekId = week.weekId,
                    lessonCount = week.total,
                    completedCount = week.completed,
                    onClick = { onNavigateToWeek(week.weekId) }
                )
            }

            item {
                MediaImportCard(
                    hasBundled = hasBundled,
                    importedCount = importedCount,
                    importState = importState,
                    onImport = {
                        importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onNavigateToLesson(1) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold
                    )
                ) {
                    Text(
                        "Продолжить",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Copyright / attribution (best practice for bundled third-party content)
            item {
                CopyrightNotice()
            }
        }
    }
}

@Composable
private fun CopyrightNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.copyright_title),
                style = MaterialTheme.typography.labelSmall,
                color = Gold
            )
            Text(
                stringResource(R.string.copyright_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class WeekEntry(
    val weekId: Int,
    val total: Int,
    val completed: Int
)

@Composable
fun ProgressBanner(
    completedCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "$completedCount/$totalCount уроков",
                style = MaterialTheme.typography.titleMedium,
                color = Gold
            )
            Text(
                "Прогресс обновляется после отметки уроков как пройденных",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = {
                    if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                },
                modifier = Modifier.fillMaxWidth(),
                color = Gold
            )
        }
    }
}

enum class ImportState {
    Idle, Importing, Done, Error
}

@Composable
fun MediaImportCard(
    hasBundled: Boolean,
    importedCount: Int,
    importState: ImportState,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Медиафайлы",
                style = MaterialTheme.typography.titleSmall,
                color = Gold
            )

            val status = when {
                importState == ImportState.Importing -> "Импорт…"
                importState == ImportState.Error -> "Не удалось импортировать. Проверьте, что выбран архив resources.zip"
                importState == ImportState.Done -> "Импортировано файлов: $importedCount"
                hasBundled -> "Аудио и учебник встроены в приложение"
                importedCount > 0 -> "Импортировано файлов: $importedCount"
                else -> "Аудио и учебник не установлены"
            }
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (importState == ImportState.Importing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Gold
                )
            }

            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                enabled = importState != ImportState.Importing
            ) {
                Text("Импортировать resources.zip")
            }
        }
    }
}

@Composable
private fun WeekCard(
    weekId: Int,
    lessonCount: Int,
    completedCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Неделя $weekId",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold
                )
                Text(
                    "Уроков: $lessonCount · пройдено: $completedCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "→",
                style = MaterialTheme.typography.headlineSmall,
                color = Gold
            )
        }
    }
}
