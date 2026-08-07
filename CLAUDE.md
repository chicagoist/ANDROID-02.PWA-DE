# CLAUDE.md

This file provides guidance to Claude Code when working with the Just German Android app.

## Project Overview

Native Android app for learning German (A2→B1) using Assimil method. Built with Kotlin + Jetpack Compose. Contains 100 lessons, 12 quizzes, 86 audio files (MP3), and PDF textbook — all stored in assets for offline-first experience.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean

# Run tests
./gradlew test

# List all tasks
./gradlew tasks
```

**Requirements:**
- JDK 17 (verified working, `JAVA_HOME` must point to JDK 17)
- Android SDK API 34 + build-tools 34.0.0
- Gradle 8.2 (wrapper included)

**APK location:** `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

**Tech Stack:**
- Kotlin 1.9.20
- Jetpack Compose (Material 3)
- Navigation Compose 2.7.6
- kotlinx.serialization 1.6.2
- Media3 ExoPlayer 1.2.1 (audio playback)
- DataStore 1.0.0 (preferences)
- Compose BOM 2024.02.00

**Package structure:**
```
com.chicagoist.justgerman
├── MainActivity.kt              # Entry point
├── JustGermanApp.kt            # NavHost setup
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt       # Main screen (12 weeks)
│   │   ├── WeekScreen.kt       # Week overview
│   │   ├── LessonScreen.kt     # Full lesson with audio
│   │   └── QuizScreen.kt       # Article quiz (der/die/das)
│   └── theme/
│       ├── Color.kt            # Zinc950 + Gold palette
│       ├── Theme.kt            # Dark Material3 theme
│       └── Type.kt             # Typography (Roboto fallback)
├── data/
│   ├── model/
│   │   ├── Lesson.kt           # @Serializable lesson model
│   │   └── Quiz.kt             # @Serializable quiz model
│   └── repository/
│       └── LessonRepository.kt # Loads JSON from assets
```

**Navigation routes:**
- `home` — main screen
- `week/{weekId}` — week 1-12 overview
- `lesson/{lessonId}` — lesson 1-100 detail
- `quiz/{quizId}` — quiz 1-12

**Data sources (assets/):**
- `lessons.json` — 100 lessons (279 KB)
- `quizzes.json` — 12 quizzes (6.4 KB)
- `resources/CD1-CD4/*.mp3` — 86 audio files
- `resources/Assimil_DE.pdf` — textbook (21 MB)

## Design System

**Colors:**
- Background: `Zinc950` (#09090B)
- Surface: `Zinc900` (#18181B)
- Primary: `Gold` (#FBBF24)
- Text: `Zinc100` (#F4F4F5)
- German flag colors: Black (#000000), Red (#DD0000), Yellow (#FFCE00)

**Typography:**
- Font: Roboto (system default) — TODO: add Ubuntu Sans for desktop parity
- Follows Material3 type scale

## Key Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` (root) | Plugin versions |
| `app/build.gradle.kts` | App config, dependencies, Compose setup |
| `settings.gradle.kts` | Repository configuration |
| `gradle.properties` | JVM args, build optimization |
| `app/src/main/AndroidManifest.xml` | App manifest, FileProvider |
| `app/src/main/res/xml/file_paths.xml` | FileProvider paths for PDF |
| `app/proguard-rules.pro` | ProGuard rules for release |

## Common Tasks

### Add new screen
1. Create `*Screen.kt` in `ui/screens/`
2. Add route constant in `JustGermanApp.kt`
3. Add `composable()` to NavHost

### Modify lesson data
Edit `app/src/main/assets/lessons.json` (regenerated from desktop PWA via `parse-lessons.js`)

### Change theme colors
Edit `ui/theme/Color.kt` and `ui/theme/Theme.kt`

### Add new dependency
Edit `app/build.gradle.kts`, add to `dependencies` block, then sync Gradle

## Important Notes

1. **Assets are huge** (428 MB total) — mostly MP3 + PDF. Build times are longer due to asset packaging.

2. **Font fallback** — Currently uses system Roboto. Ubuntu Sans font files (.ttf) are NOT included. Add them to `res/font/` and update `Type.kt` to match desktop PWA exactly.

3. **Audio player TODO** — LessonScreen shows audio path placeholder. Needs ExoPlayer integration with:
   - Speed control (0.5x - 2x)
   - Skip ±10 seconds
   - Volume control
   - Play/pause/seek

4. **PDF viewer TODO** — Need button to open `Assimil_DE.pdf` at specific page using FileProvider + external PDF app.

5. **Progress tracking TODO** — DataStore implementation for marking lessons as completed.

6. **Translations toggle TODO** — Show/hide Russian translations in lessons.

7. **Serialization** — All data models use `@Serializable`. JSON parsing via `kotlinx.serialization.json.Json`.

8. **Offline-first** — All content in assets, no network required.

## Related Project

Desktop PWA version at `C:\Projects\02.PWA-DE` (Next.js 15 + Tailwind). Lesson data extracted from there via `parse-lessons.js`.

## Testing Checklist

After changes:
- [ ] App builds without errors (`./gradlew assembleDebug`)
- [ ] Home screen shows 12 weeks
- [ ] Week screen shows lessons 1-7, 8-14, etc.
- [ ] Lesson screen displays dialog, vocabulary, grammar
- [ ] Quiz screen shows questions, tracks score
- [ ] Navigation back buttons work
- [ ] Theme colors match design (zinc-950 + gold)

## Troubleshooting

**"Plugin [compose] not found"** — Removed `kotlin.plugin.compose`, using manual `composeOptions` instead.

**"JAVA_HOME not set"** — Export/set `JAVA_HOME` to JDK 17 path.

**"SDK location not found"** — Create `local.properties` with `sdk.dir=C:\\Users\\...\\AppData\\Local\\Android\\Sdk`.

**APK too large** — Assets are 428 MB. For smaller builds, consider app bundles or dynamic delivery.
