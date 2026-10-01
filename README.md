# Just German — Android App

Native Android application for learning German (A2→B1) using the Assimil method.

## Contents

- **100 lessons** — a full course with audio, dialogues, vocabulary and grammar
- **12 quizzes** — article tests (der/die/das)
- **12 weeks** — week overviews with progress tracking
- **PDF textbook** — Assimil Deutsch ohne Mühe heute
- **86 audio files** — MP3 recordings of all lessons

## Technologies

- **Kotlin 1.9.20**
- **Jetpack Compose** — modern UI toolkit
- **Material 3** — dark theme (zinc-950 + gold)
- **Navigation Compose** — navigation between screens
- **kotlinx.serialization** — parsing of JSON lesson data
- **Media3 ExoPlayer** — audio player with speed control
- **DataStore** — progress storage
- **FileProvider** — PDF viewing

## Building

```bash
# Install dependencies and build the debug APK
./gradlew assembleDebug

# Build the release APK
./gradlew assembleRelease

# Build the release App Bundle (AAB) for the Play Store
./gradlew bundleRelease

# Install on a connected device
./gradlew installDebug

# Run the tests
./gradlew test
```

> 💡 A **local build** includes the media files if they are placed in
> `app/src/main/assets/resources/` — then the APK comes out at ~470 MB.
> The CI build (GitHub Actions) runs without media — they can be imported
> into the installed app from the `resources.zip` archive (see below).

## Data layout

- `app/src/main/assets/lessons.json` — 100 lessons (279 KB)
- `app/src/main/assets/quizzes.json` — 12 quizzes (6.4 KB)
- `app/src/main/assets/resources/CD1-CD4/` — audio files (*.mp3)
- `app/src/main/assets/resources/Assimil_DE.pdf` — textbook (21 MB)

## Media files (audio and PDF)

> ⚠️ **Media files are not stored in git** because of copyright.
> The repository contains only code and lesson data (JSON).

### How to create resources.zip

The archive must contain the `resources/` structure (or simply the files at its root):

```
resources.zip
├── Assimil_DE.pdf
├── CD1/01 Lektion.mp3 … 26 Lektion.mp3
├── CD2/27 Lektion.mp3 … 52 Lektion.mp3
├── CD3/53 Lektion.mp3 … 77 Lektion.mp3
└── CD4/78 Lektion.mp3 … 100 Lektion.mp3
```

**Option A — from the desktop PWA** (`C:\Projects\02.PWA-DE\public\resources`):

```cmd
cd /d C:\Projects\02.PWA-DE\public\resources
powershell -Command "Compress-Archive -Path * -DestinationPath C:\Projects\ANDROID-02.PWA-DE\resources.zip -Force"
```

**Option B — from the project folder** (if the files have already been copied to `app/src/main/assets/resources/`):

```cmd
cd /d C:\Projects\ANDROID-02.PWA-DE\app\src\main\assets\resources
powershell -Command "Compress-Archive -Path * -DestinationPath C:\Projects\ANDROID-02.PWA-DE\resources.zip -Force"
```

The app accepts the archive both with and without the `resources/` prefix inside it —
only `.mp3` and `.pdf` files are extracted.

### How to import into the installed app

1. Copy `resources.zip` to your device (via USB, Google Drive, a messenger, etc.).
2. Open the **Just German** app.
3. On the home screen, find the **"Media files"** card and tap **"Import resources.zip"**.
4. Select the `resources.zip` archive in the system file picker.
5. Wait for the import to finish — the card will show how many files were imported.

After the import, the audio for every lesson and the PDF textbook are available offline.
Imported files take priority over the ones bundled into the APK.

## Architecture

```
com.chicagoist.justgerman
├── MainActivity.kt                  # Entry point
├── JustGermanApp.kt                 # Navigation setup
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt           # Home (12 weeks)
│   │   ├── WeekScreen.kt           # Week overview
│   │   ├── LessonScreen.kt         # Full lesson
│   │   └── QuizScreen.kt           # Article quiz
│   └── theme/
│       ├── Color.kt                # Zinc950 + Gold palette
│       ├── Theme.kt                # Material3 dark theme
│       └── Type.kt                 # Typography (Roboto fallback)
├── data/
│   ├── model/
│   │   ├── Lesson.kt               # Lesson model
│   │   └── Quiz.kt                 # Quiz model
│   └── repository/
│       ├── LessonRepository.kt     # Loading from assets
│       ├── MediaStore.kt           # Audio/PDF (assets or import)
│       └── ProgressRepository.kt   # Progress (DataStore)
```

## TODO

- [x] Audio player with ExoPlayer (speed 0.75x–2x, ±10 sec, volume)
- [x] Text-to-speech for dialog lines via system TTS
- [x] Button to open the PDF textbook
- [x] Media import from resources.zip
- [x] Progress via DataStore (marking lessons as completed)
- [x] Russian translations of dialog lines (always visible, labelled as machine translation)
- [x] APK optimization — release as an App Bundle (AAB) + resource shrinking
- [x] Ubuntu Sans fonts (OFL 1.1) in `res/font/` — regular/medium/semibold/bold
- [x] App icon — adaptive icon + legacy for API 24–25 + monochrome (Android 13)
- [x] Opening the PDF textbook in an external viewer (Intent.ACTION_VIEW + FileProvider)

## Related project

Desktop PWA version: `C:\Projects\02.PWA-DE` (Next.js 15 + Tailwind)

## License and rights

**Just German** is a study (educational) project created by the author **chicagoist** for personal German-language learning.

### Rights to the content

> ⚠️ **All media files (MP3 audio), the PDF textbook (`Assimil_DE.pdf`) and the Assimil learning system**
> **Assimil** are the intellectual property of the rights holder:
>
> **Assimil** — Assimil SAS (France), <https://www.assimil.com/>
>
> **Distribution of these materials is prohibited.** They are included in the app
> exclusively for personal, non-commercial use. For this reason the media files
> are not stored in git (see `.gitignore`).

### App code

The source code (Kotlin, Compose, configuration, JSON data) was written by the author
`chicagoist` and is distributed under the **[MIT](LICENSE) license**.
The full copyright notice is in the [`NOTICE`](NOTICE) file.

> ⚠️ The MIT license covers **only the app code** and does **not** extend to the
> Assimil content (audio, textbook, method), which remains the property of the
> rights holder and may not be distributed.

---

# Русская версия

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

# Собрать release App Bundle (AAB) для Play Store
./gradlew bundleRelease

# Установить на подключенное устройство
./gradlew installDebug

# Запустить тесты
./gradlew test
```

> 💡 **Локальная сборка** включает медиафайлы, если они лежат в
> `app/src/main/assets/resources/` — тогда APK получается ~470 МБ.
> Сборка в CI (GitHub Actions) идёт без медиа — их можно импортировать
> в установленное приложение из архива `resources.zip` (см. ниже).

## Структура данных

- `app/src/main/assets/lessons.json` — 100 уроков (279 KB)
- `app/src/main/assets/quizzes.json` — 12 квизов (6.4 KB)
- `app/src/main/assets/resources/CD1-CD4/` — аудиофайлы (*.mp3)
- `app/src/main/assets/resources/Assimil_DE.pdf` — учебник (21 MB)

## Медиафайлы (аудио и PDF)

> ⚠️ **Медиафайлы не хранятся в git** из-за авторских прав.
> В репозитории есть только код и данные уроков (JSON).

### Как создать resources.zip

Архив должен содержать структуру `resources/` (или просто файлы в корне):

```
resources.zip
├── Assimil_DE.pdf
├── CD1/01 Lektion.mp3 … 26 Lektion.mp3
├── CD2/27 Lektion.mp3 … 52 Lektion.mp3
├── CD3/53 Lektion.mp3 … 77 Lektion.mp3
└── CD4/78 Lektion.mp3 … 100 Lektion.mp3
```

**Вариант A — из десктопной PWA** (`C:\Projects\02.PWA-DE\public\resources`):

```cmd
cd /d C:\Projects\02.PWA-DE\public\resources
powershell -Command "Compress-Archive -Path * -DestinationPath C:\Projects\ANDROID-02.PWA-DE\resources.zip -Force"
```

**Вариант B — из папки проекта** (если файлы уже скопированы в `app/src/main/assets/resources/`):

```cmd
cd /d C:\Projects\ANDROID-02.PWA-DE\app\src\main\assets\resources
powershell -Command "Compress-Archive -Path * -DestinationPath C:\Projects\ANDROID-02.PWA-DE\resources.zip -Force"
```

Приложение принимает архив и с префиксом `resources/` внутри, и без него —
распаковываются только `.mp3` и `.pdf` файлы.

### Как импортировать в установленное приложение

1. Скопируйте `resources.zip` на устройство (через USB, Google Drive, мессенджер и т.д.).
2. Откройте приложение **Just German**.
3. На главном экране найдите карточку **«Медиафайлы»** и нажмите **«Импортировать resources.zip»**.
4. В системном окне выбора файлов укажите архив `resources.zip`.
5. Дождитесь окончания импорта — в карточке появится количество импортированных файлов.

После импорта аудио всех уроков и учебник PDF доступны офлайн.
Импортированные файлы имеют приоритет над встроенными в APK.

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
│       ├── LessonRepository.kt     # Загрузка из assets
│       ├── MediaStore.kt           # Аудио/PDF (assets или импорт)
│       └── ProgressRepository.kt   # Прогресс (DataStore)
```

## TODO

- [x] Аудиоплеер с ExoPlayer (скорость 0.75x-2x, ±10сек, громкость)
- [x] Озвучка реплик диалога через системный TTS
- [x] Кнопка открытия учебника PDF
- [x] Импорт медиафайлов из resources.zip
- [x] Прогресс через DataStore (отметка уроков как пройденных)
- [x] Русские переводы реплик диалога (всегда видны, с пометкой машинного перевода)
- [x] Оптимизация APK — release в формате App Bundle (AAB) + сжатие ресурсов
- [x] Шрифты Ubuntu Sans (OFL 1.1) в `res/font/` — regular/medium/semibold/bold
- [x] Иконка приложения — adaptive icon + legacy для API 24-25 + monochrome (Android 13)
- [x] Открытие учебника PDF во внешнем просмотрщике (Intent.ACTION_VIEW + FileProvider)

## Связанный проект

Десктопная PWA версия: `C:\Projects\02.PWA-DE` (Next.js 15 + Tailwind)

## Лицензия и права

**Just German** — учебный (образовательный) проект, созданный автором **chicagoist** для личного изучения немецкого языка.

### Права на контент

> ⚠️ **Все медиафайлы (аудио MP3), учебник PDF (`Assimil_DE.pdf`) и обучающая система**
> **Assimil** являются интеллектуальной собственностью правообладателя:
>
> **Assimil** — Assimil SAS (Франция), <https://www.assimil.com/>
>
> **Распространение этих материалов запрещено.** Они включены в приложение
> исключительно для личного, некоммерческого использования. По этой причине
> медиафайлы не хранятся в git (см. `.gitignore`).

### Код приложения

Исходный код (Kotlin, Compose, конфигурация, JSON-данные) создан автором
`chicagoist` и распространяется под **лицензией [MIT](LICENSE)**.
Полное уведомление о правах — в файле [`NOTICE`](NOTICE).

> ⚠️ Лицензия MIT покрывает **только код приложения** и **не** распространяется
> на контент Assimil (аудио, учебник, метод), который остаётся собственностью
> правообладателя и не подлежит распространению.
