# Just German — Android App

Нативное Android-приложение для изучения немецкого языка A2→B1 по методу Assimil.

## Структура

- **100 уроков** — полный курс с аудио, диалогами, лексикой, грамматикой
- **12 квизов** — тесты на артикли (der/die/das)
- **12 недель** — обзоры недель с прогрессом
- **PDF учебник** — Assimil Deutsch ohne Mühe heute
- **86 аудиофайлов** — MP3 записи всех уроков

## Технологии

- **Kotlin 1.9.20**
- **Jetpack Compose** — современный UI toolkit
- **Material 3** — темная тема (zinc-950 + gold)
- **Navigation Compose** — навигация между экранами
- **kotlinx.serialization** — парсинг JSON данных уроков
- **Media3 ExoPlayer** — аудиоплеер с управлением скорости
- **DataStore** — хранение прогресса
- **FileProvider** — просмотр PDF

## Сборка

```bash
# Установить зависимости и собрать debug APK
./gradlew assembleDebug

# Собрать release APK
./gradlew assembleRelease

# Установить на подключенное устройство
./gradlew installDebug

# Запустить тесты
./gradlew test
```

## Структура данных

- `app/src/main/assets/lessons.json` — 100 уроков (279 KB)
- `app/src/main/assets/quizzes.json` — 12 квизов (6.4 KB)
- `app/src/main/assets/resources/CD1-CD4/` — аудиофайлы (*.mp3)
- `app/src/main/assets/resources/Assimil_DE.pdf` — учебник (21 MB)

## Архитектура

```
com.chicagoist.justgerman
├── MainActivity.kt                  # Точка входа
├── JustGermanApp.kt                 # Navigation setup
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt           # Главная (12 недель)
│   │   ├── WeekScreen.kt           # Обзор недели
│   │   ├── LessonScreen.kt         # Полный урок
│   │   └── QuizScreen.kt           # Квиз на артикли
│   └── theme/
│       ├── Color.kt                # Zinc950 + Gold палитра
│       ├── Theme.kt                # Material3 темная тема
│       └── Type.kt                 # Typography (Roboto fallback)
├── data/
│   ├── model/
│   │   ├── Lesson.kt               # Модель урока
│   │   └── Quiz.kt                 # Модель квиза
│   └── repository/
│       └── LessonRepository.kt     # Загрузка из assets
```

## TODO

- [ ] Реализовать аудиоплеер с ExoPlayer (скорость 0.5x-2x, ±10сек)
- [ ] Добавить кнопку открытия PDF на нужной странице
- [ ] Реализовать прогресс через DataStore (отметка уроков как пройденных)
- [ ] Добавить показ/скрытие переводов
- [ ] Оптимизировать размер APK (сжатие MP3, PDF)
- [ ] Добавить реальные шрифты Ubuntu Sans (сейчас Roboto fallback)
- [ ] Настроить иконку приложения (сейчас placeholder)

## Связанный проект

Десктопная PWA версия: `C:\Projects\02.PWA-DE` (Next.js 15 + Tailwind)

## Лицензия

Private project.
