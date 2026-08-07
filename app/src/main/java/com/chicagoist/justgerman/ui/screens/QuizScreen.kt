package com.chicagoist.justgerman.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chicagoist.justgerman.data.repository.LessonRepository
import com.chicagoist.justgerman.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { LessonRepository(context) }
    val quiz = remember { repository.getQuiz(quizId) }

    var currentQuestion by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    if (quiz == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Квиз не найден")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Квиз — Неделя ${quiz.week}")
                        Text(
                            "$score/${currentQuestion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (currentQuestion >= quiz.questions.size) {
            // Quiz completed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Квиз завершён!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Gold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Ваш результат: $score из ${quiz.questions.size}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold
                        )
                    ) {
                        Text("Завершить")
                    }
                }
            }
        } else {
            val question = quiz.questions[currentQuestion]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { currentQuestion.toFloat() / quiz.questions.size },
                    modifier = Modifier.fillMaxWidth(),
                    color = Gold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Вопрос ${currentQuestion + 1} из ${quiz.questions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "Welcher Artikel? ___ ${question.word}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                listOf("der", "die", "das").forEach { article ->
                    ArticleButton(
                        article = article,
                        selected = selectedAnswer == article,
                        onClick = {
                            // Guard against double-taps while auto-advance is pending:
                            // runs synchronously, so the first tap wins and locks the rest.
                            if (selectedAnswer == null) {
                                selectedAnswer = article
                                if (article == question.article) {
                                    score++
                                }
                                // Auto-advance after 500ms; scope is cancelled
                                // when the screen leaves composition
                                scope.launch {
                                    delay(500)
                                    selectedAnswer = null
                                    currentQuestion++
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ArticleButton(
    article: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Gold.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            article,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
