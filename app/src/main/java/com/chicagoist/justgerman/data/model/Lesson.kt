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

package com.chicagoist.justgerman.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: Int,
    val week: Int,
    val phase: String,
    val title: String = "",
    // Weekly themes shared by all 7 lessons of a week
    // (e.g. L1-L7 all have "Im Café · Das Restaurant · Im Park · ...").
    // UI uses this in WeekScreen header, NOT in per-lesson card / TopAppBar.
    val topics: List<String>,
    val audioPath: String,
    val dialog: List<DialogLine>,
    val vocabulary: List<VocabularyItem>,
    val grammar: String,
    val essence: String,
    val difficulties: List<String>,
    val mistakes: List<String>,
    val tips: List<String>
)

@Serializable
data class DialogLine(
    val german: String,
    val pronunciation: String,
    val russian: String
)

@Serializable
data class VocabularyItem(
    val german: String,
    val russian: String
)

@Serializable
data class LessonsData(
    val lessons: List<Lesson>,
    val metadata: Metadata
)

@Serializable
data class Metadata(
    val totalLessons: Int,
    val generatedAt: String,
    val version: String
)
