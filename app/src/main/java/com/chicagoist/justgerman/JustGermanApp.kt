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

package com.chicagoist.justgerman

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chicagoist.justgerman.ui.screens.*

@Composable
fun JustGermanApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToLesson = { lessonId ->
                    navController.navigate("lesson/$lessonId")
                },
                onNavigateToWeek = { weekId ->
                    navController.navigate("week/$weekId")
                }
            )
        }

        composable("lesson/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull() ?: 1
            LessonScreen(
                lessonId = lessonId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("quiz/{quizId}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")?.toIntOrNull() ?: 1
            QuizScreen(
                quizId = quizId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("week/{weekId}") { backStackEntry ->
            val weekId = backStackEntry.arguments?.getString("weekId")?.toIntOrNull() ?: 1
            WeekScreen(
                weekId = weekId,
                onNavigateToLesson = { lessonId ->
                    navController.navigate("lesson/$lessonId")
                },
                onNavigateToQuiz = { quizId ->
                    navController.navigate("quiz/$quizId")
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
