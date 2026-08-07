package com.chicagoist.justgerman.data.repository

import android.content.Context
import com.chicagoist.justgerman.data.model.*
import kotlinx.serialization.json.Json
import java.io.IOException

class LessonRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private var cachedLessons: List<Lesson>? = null
    private var cachedQuizzes: List<Quiz>? = null

    fun getLessons(): List<Lesson> {
        if (cachedLessons != null) return cachedLessons!!

        return try {
            val jsonString = context.assets.open("lessons.json")
                .bufferedReader()
                .use { it.readText() }
            val data = json.decodeFromString<LessonsData>(jsonString)
            cachedLessons = data.lessons
            data.lessons
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun getLesson(id: Int): Lesson? {
        return getLessons().find { it.id == id }
    }

    fun getLessonsByWeek(week: Int): List<Lesson> {
        return getLessons().filter { it.week == week }
    }

    fun getQuizzes(): List<Quiz> {
        if (cachedQuizzes != null) return cachedQuizzes!!

        return try {
            val jsonString = context.assets.open("quizzes.json")
                .bufferedReader()
                .use { it.readText() }
            val data = json.decodeFromString<QuizzesData>(jsonString)
            cachedQuizzes = data.quizzes
            data.quizzes
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun getQuiz(id: Int): Quiz? {
        return getQuizzes().find { it.id == id }
    }
}
