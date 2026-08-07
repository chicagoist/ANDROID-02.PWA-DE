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
import androidx.compose.ui.unit.dp
import com.chicagoist.justgerman.data.repository.MediaStore
import com.chicagoist.justgerman.ui.theme.Gold
import com.chicagoist.justgerman.ui.theme.Zinc800
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLesson: (Int) -> Unit,
    onNavigateToQuiz: (Int) -> Unit,
    onNavigateToWeek: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaStore = remember { MediaStore(context) }

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
            item {
                Text(
                    "12 недель интенсива",
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

            items((1..12).toList()) { week ->
                WeekCard(
                    week = week,
                    onClick = { onNavigateToWeek(week) }
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
                    Text("Начать урок 1")
                }
            }
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
fun WeekCard(
    week: Int,
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
            Column {
                Text(
                    "Неделя $week",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Уроки ${(week - 1) * 7 + 1}–${minOf(week * 7, 100)}",
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
