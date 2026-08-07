package com.chicagoist.justgerman.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chicagoist.justgerman.data.repository.LessonRepository
import com.chicagoist.justgerman.ui.theme.FlagBlack
import com.chicagoist.justgerman.ui.theme.Gold
import com.chicagoist.justgerman.ui.theme.QuizCorrect
import com.chicagoist.justgerman.ui.theme.QuizWrong
import com.chicagoist.justgerman.ui.theme.Zinc800

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
    var quizFinished by remember { mutableStateOf(false) }

    if (quiz == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Квиз не найден")
        }
        return
    }

    val totalQuestions = quiz.questions.size
    val answered = selectedAnswer != null

    // Reset scroll to the top whenever the question changes.
    val scrollState = rememberScrollState()
    LaunchedEffect(currentQuestion) {
        scrollState.animateScrollTo(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Квиз — Неделя ${quiz.week}")
                        Text(
                            "Вопрос ${(currentQuestion + 1).coerceAtMost(totalQuestions)} из $totalQuestions",
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
        if (quizFinished) {
            QuizResults(
                score = score,
                total = totalQuestions,
                onRestart = {
                    currentQuestion = 0
                    selectedAnswer = null
                    score = 0
                    quizFinished = false
                },
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            val question = quiz.questions[currentQuestion]
            val isCorrect = selectedAnswer == question.article

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress bar
                LinearProgressIndicator(
                    progress = {
                        (currentQuestion + if (answered) 1 else 0).toFloat() / totalQuestions
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = Gold
                )

                // Score counter (like the PWA)
                Text(
                    "Очки: $score / $totalQuestions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Question with a blank for the article (like the PWA)
                Text(
                    "Welcher Artikel?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "___ ${question.word}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Answer buttons with green/red feedback
                listOf("der", "die", "das").forEach { article ->
                    val isSelected = selectedAnswer == article
                    val isCorrectAnswer = article == question.article

                    val containerColor = when {
                        !answered -> Zinc800
                        isCorrectAnswer -> QuizCorrect
                        isSelected -> QuizWrong
                        else -> Zinc800
                    }
                    val contentColor = when {
                        !answered -> MaterialTheme.colorScheme.onSurface
                        isCorrectAnswer || isSelected -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Button(
                        onClick = {
                            if (selectedAnswer == null) {
                                selectedAnswer = article
                                if (article == question.article) {
                                    score++
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                        )
                    ) {
                        Text(
                            article,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Explanation after answering (like the PWA)
                if (answered) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) QuizCorrect.copy(alpha = 0.12f) else QuizWrong.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                if (isCorrect) "Правильно!" else "Неправильно",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isCorrect) QuizCorrect else QuizWrong
                            )
                            Text(
                                "${question.article} ${question.word}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentQuestion + 1 >= totalQuestions) {
                                quizFinished = true
                            } else {
                                selectedAnswer = null
                                currentQuestion++
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = FlagBlack
                        )
                    ) {
                        Text(
                            if (currentQuestion + 1 >= totalQuestions) "Показать результат" else "Следующий вопрос",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizResults(
    score: Int,
    total: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (total > 0) (score * 100 / total) else 0

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Квиз завершён!",
            style = MaterialTheme.typography.headlineMedium,
            color = Gold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "$score из $total",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "$percent%",
            style = MaterialTheme.typography.titleLarge,
            color = when {
                percent >= 80 -> QuizCorrect
                percent >= 50 -> Gold
                else -> QuizWrong
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = FlagBlack
            )
        ) {
            Text("Пройти заново", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выйти")
        }
    }
}
