# Just German — Android WebView Wrapper

Это нативная Android-обёртка (WebView) вокруг статического сайта, сгенерированного из проекта `C:\Projects\02.PWA-DE`.

## Структура

```
android/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/chicagoist/justgerman/
│       │   ├── MainActivity.kt
│       │   └── SpaPathHandler.kt
│       └── assets/www/          ← статический сайт (432 MB)
```

## Что внутри

- **Kotlin + AndroidX WebView** — отображает сайт из локальных ассетов.
- **WebViewAssetLoader** — сервит файлы из `assets/www/` по адресу `https://appassets.androidplatform.net/`.
- **SpaPathHandler** — поддерживает Next.js-маршуты: `/lesson/1`, `/quiz/1`, `/week/1` и т.д.
- **FileProvider** — открывает PDF из ассетов во внешнем приложении.

## Требования

- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17 или JDK 21
- Android SDK API 34
- Gradle 8.2+

## Как собрать

1. Откройте папку `android` в Android Studio.
2. Дождитесь окончания Gradle sync.
3. Выберите **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

Готовый APK появится в:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions CI

В `.github/workflows/android.yml` настроен автоматический сборщик, который при каждом `push` в `main` собирает debug APK и сохраняет его в артефакты.

Чтобы запустить CI:

1. Создайте новый публичный репозиторий на GitHub.
2. Выполните в терминале:

```bash
cd "C:\Projects\ANDROID-02.PWA-DE"
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git add .
git commit -m "Initial commit: Just German Android WebView wrapper"
git push -u origin main
```

3. Откройте вкладку **Actions** в GitHub — сборка начнётся автоматически.
4. После успешного завершения скачайте артефакт `app-debug.apk`.

## Ограничения

- Приложение содержит все статические файлы, включая MP3 и PDF, поэтому размер APK около **430 MB**.
- Service Worker из оригинальной PWA не нужен, так как файлы уже находятся локально.
- Некоторые динамические функции Next.js, зависящие от серверной части (например, `/api/progress`), не работают. Прогресс можно хранить через `localStorage`/`IndexedDB` WebView.

## Оффлайн

После установки приложение работает полностью офлайн: все уроки, квизы, аудио и PDF уже внутри APK.
