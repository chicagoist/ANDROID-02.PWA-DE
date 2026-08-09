/*
 * Just German — учебный проект
 * Copyright (c) 2026 chicagoist
 *
 * SPDX-License-Identifier: MIT
 *
 * Аудио, учебник и метод Assimil принадлежат правообладателю Assimil SAS
 * (Франция, assimil.com). Распространение этих материалов запрещено.
 * См. файл NOTICE.
 */

package com.chicagoist.justgerman.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Quiz(
    val id: Int,
    val week: Int,
    val questions: List<QuizQuestion>
)

@Serializable
data class QuizQuestion(
    val word: String,
    val article: String
)

@Serializable
data class QuizzesData(
    val quizzes: List<Quiz>,
    val metadata: QuizMetadata
)

@Serializable
data class QuizMetadata(
    val totalQuizzes: Int,
    val questionsPerQuiz: Int,
    val generatedAt: String,
    val version: String
)
