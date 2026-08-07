package com.chicagoist.justgerman.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: Int,
    val week: Int,
    val phase: String,
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
