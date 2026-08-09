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
│       ├── ProgressRepository.kt   # Прогресс (DataStore)
│       └── SettingsRepository.kt   # Настройки (DataStore)
```

## TODO

- [x] Аудиоплеер с ExoPlayer (скорость 0.75x-2x, ±10сек, громкость)
- [x] Озвучка реплик диалога через системный TTS
- [x] Кнопка открытия учебника PDF
- [x] Импорт медиафайлов из resources.zip
- [x] Прогресс через DataStore (отметка уроков как пройденных)
- [x] Показ/скрытие переводов (переключатель в уроке)
- [x] Оптимизация APK — release в формате App Bundle (AAB) + сжатие ресурсов
- [x] Шрифты Ubuntu Sans (OFL 1.1) в `res/font/` — regular/medium/semibold/bold
- [x] Иконка приложения — adaptive icon + legacy для API 24-25 + monochrome (Android 13)
- [x] Открытие PDF на нужной странице урока (встроенный просмотрщик на PdfRenderer)

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
