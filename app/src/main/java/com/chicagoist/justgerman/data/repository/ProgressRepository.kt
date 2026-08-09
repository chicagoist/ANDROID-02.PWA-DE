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

package com.chicagoist.justgerman.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "progress")

class ProgressRepository(private val context: Context) {

    private val completedLessonsKey = stringSetPreferencesKey("completed_lessons")

    /** Emits the set of completed lesson ids whenever it changes. */
    val completedLessons: Flow<Set<Int>> = context.dataStore.data
        .map { prefs ->
            prefs[completedLessonsKey]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet()
        }

    suspend fun markCompleted(lessonId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[completedLessonsKey]?.toMutableSet() ?: mutableSetOf()
            current.add(lessonId.toString())
            prefs[completedLessonsKey] = current
        }
    }
}
