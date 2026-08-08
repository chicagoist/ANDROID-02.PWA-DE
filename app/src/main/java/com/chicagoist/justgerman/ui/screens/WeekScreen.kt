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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chicagoist.justgerman.data.repository.LessonRepository
import com.chicagoist.justgerman.data.repository.ProgressRepository
import com.chicagoist.justgerman.ui.theme.Gold
import com.chicagoist.justgerman.ui.theme.QuizCorrect
import com.chicagoist.justgerman.ui.theme.Zinc800

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    weekId: Int,
    onNavigateToLesson: (Int) -> Unit,
    onNavigateToQuiz: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { LessonRepository(context) }
    val lessons = remember { repository.getLessonsByWeek(weekId) }
    val topics = remember(lessons) {
        lessons.flatMap { it.topics }.distinct().take(5)
    }

    // Lesson completion progress (DataStore)
    val progressRepository = remember { ProgressRepository(context) }
    val completedLessons by progressRepository.completedLessons
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val completedInWeek = remember(completedLessons) {
        lessons.count { it.id in completedLessons }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {                        Column {
                            Text("Неделя $weekId")
                            Text(
                                topics.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Пройдено $completedInWeek из ${lessons.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gold
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lessons) { lesson ->
                Card(
                    onClick = { onNavigateToLesson(lesson.id) },
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
                                "Урок ${lesson.id}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Gold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                lesson.title.ifBlank { lesson.topics.joinToString(" · ") },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lesson.id in completedLessons) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Пройден",
                                tint = QuizCorrect
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quiz link (like the PWA)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    onClick = { onNavigateToQuiz(weekId) },
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
                            "Квиз недели $weekId",
                            style = MaterialTheme.typography.titleMedium,
                            color = Gold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Проверьте свои знания · 10 вопросов →",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
