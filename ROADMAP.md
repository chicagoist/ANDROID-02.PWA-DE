# ROADMAP — modernization plan for Just German

> Functionally the codebase is **complete**: 100 lessons, 12 quizzes, audio player
> (ExoPlayer), TTS speech synthesis, PDF textbook (external viewer), progress (DataStore),
> RU translations of the lines — everything has been tested on a smartphone.
> Below is the modernization queue ordered by priority.

**Status marker:** last synchronized snapshot — `main @ 0df8517`
(docs: align CLAUDE.md/README.md; remove dead analyze-pdf.js), CI is green.

---

## 🔴 P0 — The release lags behind the code (top priority)

| # | Task | Why / details | Status |
|---|------|---------------|--------|
| 1 | **Version bump + release v1.1.0** | Release v1.0.0 was built from the old snapshot `15d3d7e` — **without** RU translations, the MIT license, new icons and security fixes. Current main = `0df8517`. Needed: `versionCode 1→2`, `versionName 1.0.0→1.1.0` in `app/build.gradle.kts`, rebuild the signed APK + AAB, publish release v1.1.0 | ⬜ |
| 2 | **A proper release description** | Right now the release body is only a link to the changelog. Needed: a title, an app description, installation instructions, and a warning that "media (audio/PDF) are not included and are imported separately via resources.zip" | ⬜ |

## 🟠 P1 — Engineering quality

| # | Task | Details | Status |
|---|------|---------|--------|
| 3 | **Unit tests (currently 0)** | Minimum: `LessonRepository` (JSON parsing, 100 lessons / 507 lines), `ProgressRepository` (DataStore), `MediaStore` (audio/PDF resolution). Stack: JUnit4 + kotlinx-coroutines-test. CI only runs build + lint — tests would close this gap | ⬜ |
| 4 | ~~Clean up outdated documentation~~ | ~~CLAUDE.md and README.md referenced the removed `PdfViewerScreen.kt`, `PdfPageMap.kt`, `lesson-page-map.json`, `SettingsRepository`, the `pdf/{lessonId}` route, and the "Translations toggle"~~ | ✅ **done** (`0df8517`) |
| 5 | ~~Dead code audit~~ | ~~`tools/analyze-pdf.js` + `tools/lesson-page-map.json` — dead after the PDF viewer was removed~~ | ✅ **done** (`tools/analyze-pdf.js` removed in `0df8517`; `lesson-page-map.json` removed earlier; `tools/build/` — build artifacts, not in git) |

## 🟡 P2 — Product

| # | Task | Details | Status |
|---|------|---------|--------|
| 6 | **Quality of the RU translations** | All 507 lines are marked with the `[auto]` suffix (machine-translated). Replace them with real translations (a manual parser based on the Russian edition of Assimil) or drop the suffix from the UI — it depends on the decision | ⬜ |
| 7 | **APK size** | Debug is 445 MB (media). Fine for personal use, but a decision is needed: keep media in assets or make it import-only via `resources.zip` | ⬜ |
| 8 | **Play Store publication** | The AAB is ready, but it is legally risky: **distribution of Assimil content is prohibited**. This requires a separate discussion (possibly a code-only version without media) | ⬜ |

## 🟢 P3 — Long-term improvements

| # | Task | Status |
|---|------|--------|
| 9 | Light theme / Material You dynamic color | ⬜ |
| 10 | Onboarding screen, progress statistics, reminders | ⬜ |

---

## ⚠️ Honest assessment

The main debt is **P0** (release v1.1.0) and **P1-3** (unit tests). The code is
functionally complete and has been verified on a device, but:

- the release does not reflect the current code (v1.0.0 was built from an old snapshot);
- there are zero tests (CI only checks build + lint);
- (the documentation has already been cleaned up — see P1-4/5).

**The nearest concrete step:** tasks #1+2 — version bump → v1.1.0 with a full
release description. The alternative is #3 (unit tests).

---

## ⚙️ Operational notes

- **Debug ↔ Release on a device**: these builds are signed with different keys
  (`debug.keystore` vs `release.jks`) and **cannot** replace each other via
  `adb install -r` (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). When switching modes
  you need `adb uninstall` (which wipes the DataStore data on the device).

## Related documents

- `README.md` — overview, build, media, license
- `CLAUDE.md` — guide for agents/development
- `.github/workflows/android.yml` / `release.yml` — CI/CD
- `NOTICE` / `LICENSE` — rights (code MIT, Assimil content is the property of the rights holder, distribution prohibited)

---

# Русская версия

# ROADMAP — план дальнейшей модернизации Just German

> Функционально кодовая база **завершена**: 100 уроков, 12 квизов, аудиоплеер
> (ExoPlayer), озвучка TTS, учебник PDF (внешний просмотрщик), прогресс (DataStore),
> RU-переводы реплик — всё проверено на смартфоне.
> Ниже — очередь модернизации по приоритетам.

**Статусная отметка:** последний синхронизированный снимок — `main @ 0df8517`
(docs: align CLAUDE.md/README.md; remove dead analyze-pdf.js), CI зелёный.

---

## 🔴 P0 — Релиз отстаёт от кода (главное)

| # | Задача | Почему / детали | Статус |
|---|--------|-----------------|--------|
| 1 | **Version bump + релиз v1.1.0** | Релиз v1.0.0 собран из старого снимка `15d3d7e` — **без** RU-переводов, MIT-лицензии, новых иконок, security-фиксов. Текущий main = `0df8517`. Нужно: `versionCode 1→2`, `versionName 1.0.0→1.1.0` в `app/build.gradle.kts`, пересборка подписанного APK + AAB, публикация релиза v1.1.0 | ⬜ |
| 2 | **Полноценное описание релиза** | Сейчас body релиза = только ссылка на changelog. Нужно: название, описание приложения, инструкция установки, предупреждение «медиа (аудио/PDF) не включены и импортируются отдельно через resources.zip» | ⬜ |

## 🟠 P1 — Инженерное качество

| # | Задача | Детали | Статус |
|---|--------|--------|--------|
| 3 | **Юнит-тесты (сейчас 0)** | Минимум: `LessonRepository` (парсинг JSON, 100 уроков / 507 реплик), `ProgressRepository` (DataStore), `MediaStore` (резолвинг аудио/PDF). Стек: JUnit4 + kotlinx-coroutines-test. CI гоняет только сборку + lint — тесты закроют пробел | ⬜ |
| 4 | ~~Вычистить устаревшую документацию~~ | ~~CLAUDE.md и README.md ссылались на удалённые `PdfViewerScreen.kt`, `PdfPageMap.kt`, `lesson-page-map.json`, `SettingsRepository`, роут `pdf/{lessonId}`, «переключатель Переводов»~~ | ✅ **сделано** (`0df8517`) |
| 5 | ~~Аудит мёртвого кода~~ | ~~`tools/analyze-pdf.js` + `tools/lesson-page-map.json` — мёртвые после удаления PDF-вьювера~~ | ✅ **сделано** (`tools/analyze-pdf.js` удалён в `0df8517`; `lesson-page-map.json` убран раньше; `tools/build/` — артефакты сборки, не в git) |

## 🟡 P2 — Продукт

| # | Задача | Детали | Статус |
|---|--------|--------|--------|
| 6 | **Качество RU-переводов** | Все 507 реплик помечены суффиксом `[auto]` (машинные). Заменить на реальные переводы (ручной парсер по русскому изданию Assimil) или убрать суффикс из UI — зависит от решения | ⬜ |
| 7 | **Размер APK** | Debug 445 МБ (медиа). Для личного использования ок, но нужно решение: держать медиа в assets или только импортом через `resources.zip` | ⬜ |
| 8 | **Публикация в Play Store** | AAB готов, но юридически рискованно: **контент Assimil распространять запрещено**. Требуется отдельное обсуждение (возможно, только code-only версия без медиа) | ⬜ |

## 🟢 P3 — Долгосрочные улучшения

| # | Задача | Статус |
|---|--------|--------|
| 9 | Светлая тема / Material You dynamic color | ⬜ |
| 10 | Онбординг-экран, статистика прогресса, напоминания | ⬜ |

---

## ⚠️ Честная оценка

Главный долг — **P0** (релиз v1.1.0) и **P1-3** (юнит-тесты). Код функционально
завершён и проверен на устройстве, но:

- релиз не отражает текущий код (v1.0.0 собран из старого снимка);
- тестов — ноль (CI проверяет только сборку + lint);
- (документация уже приведена в порядок — см. P1-4/5).

**Ближайший конкретный шаг:** задача №1+2 — version bump → v1.1.0 с полноценным
описанием релиза. Альтернатива — №3 (юнит-тесты).

---

## ⚙️ Операционные заметки

- **Debug ↔ Release на устройстве**: эти сборки подписаны разными ключами
  (`debug.keystore` vs `release.jks`) и **не могут** заменять друг друга через
  `adb install -r` (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). При переключении
  режимов нужно `adb uninstall` (стирает данные DataStore на устройстве).

## Связанные документы

- `README.md` — обзор, сборка, медиа, лицензия
- `CLAUDE.md` — руководство для агентов/разработки
- `.github/workflows/android.yml` / `release.yml` — CI/CD
- `NOTICE` / `LICENSE` — права (код MIT, контент Assimil — собственность правообладателя, распространение запрещено)
