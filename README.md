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
│       └── assets/www/          ← статический сайт (~8 MB без медиа)
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

## Local Setup (Large Media Assets)

> ⚠️ **Чтобы git-репозиторий оставался компактным, большие медиафайлы исключены из индекса.**
> В репозитории хранится только HTML/CSS/JS и небольшие ресурсы. Без аудио и PDF локальная сборка пройдёт, но приложение не будет воспроизводить аудио и не откроет учебник.

### Какие файлы нужны

Скачайте архив с медиафайлами (например, `resources.zip`) и распакуйте его так, чтобы структура папки `android/app/src/main/assets/www/resources/` выглядела следующим образом:

```
android/app/src/main/assets/www/resources/
├── .gitkeep
├── Assimil_DE.pdf
├── CD1/
│   ├── 01.mp3
│   ├── 02.mp3
│   └── ...
├── CD2/
│   ├── 27.mp3
│   ├── 29.mp3
│   └── ...
├── CD3/
│   ├── 53.mp3
│   ├── 57.mp3
│   └── ...
└── CD4/
    ├── 78.mp3
    ├── 85.mp3
    └── ...
```

### Важные пути

- **Учебник PDF**: `android/app/src/main/assets/www/resources/Assimil_DE.pdf`
- **Аудио CD1**: `android/app/src/main/assets/www/resources/CD1/*.mp3`
- **Аудио CD2**: `android/app/src/main/assets/www/resources/CD2/*.mp3`
- **Аудио CD3**: `android/app/src/main/assets/www/resources/CD3/*.mp3`
- **Аудио CD4**: `android/app/src/main/assets/www/resources/CD4/*.mp3`

### Проверка

После разархивирования убедитесь, что файлы находятся в нужных папках. В Git Bash или PowerShell выполните:

```bash
cd "C:\Projects\ANDROID-02.PWA-DE\android\app\src\main\assets\www\resources"
ls -R
```

Вы должны увидеть файл `Assimil_DE.pdf` и папки `CD1`, `CD2`, `CD3`, `CD4` с MP3-файлами.

### Почему так

- Файлы MP3 и PDF исключены из `.gitignore` по маскам `*.mp3` и `*.pdf` внутри `android/app/src/main/assets/www/resources/`.
- При следующем `git push` они не попадут в репозиторий, но останутся на вашем диске.
- GitHub Actions CI будет собирать APK **без** этих медиафайлов, так как они не хранятся в git. Для CI-сборок с медиафайлами можно либо добавить шаг загрузки архива, либо выполнять release-сборку локально.

## Как собрать

1. Убедитесь, что медиафайлы разархивированы в `android/app/src/main/assets/www/resources/` (см. раздел выше).
2. Откройте папку `android` в Android Studio.
3. Дождитесь окончания Gradle sync.
4. Выберите **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

Готовый APK появится в:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions CI

В `.github/workflows/android.yml` настроен автоматический сборщик, который при каждом `push` в `main` собирает debug APK и сохраняет его в артефакты.

> ⚠️ Сборка CI не включает аудио и PDF, поэтому артефакт `app-debug.apk` будет работать только для проверки UI/UX.

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

## Release-сборка с подписью

CI поддерживает подписанный release APK/AAB через GitHub Secrets.

### 1. Сгенерируйте keystore

```bash
keytool -genkey -v \
  -keystore release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias upload
```

При выполнении укажите:
- пароль для keystore
- ваши данные (CN, OU и т.д. — можно оставить значения по умолчанию)
- пароль для alias (можно совпадающий с паролем keystore)

### 2. Закодируйте keystore в base64

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Content release_base64.txt
```

Git Bash / WSL / macOS / Linux:

```bash
base64 -w 0 release.jks > release_base64.txt
```

Содержимое файла `release_base64.txt` — это одна длинная строка.

### 3. Добавьте секреты в GitHub

Откройте страницу репозитория → **Settings → Secrets and variables → Actions → New repository secret** и добавьте четыре секрета:

| Название секрета | Значение |
|------------------|----------|
| `KEYSTORE_BASE64` | Вся строка из `release_base64.txt` |
| `KEYSTORE_PASSWORD` | Пароль от keystore |
| `KEY_ALIAS` | `upload` (или тот alias, который указали) |
| `KEY_PASSWORD` | Пароль от alias |

### 4. Запустите сборку

После следующего `push` в `main` CI выполнит дополнительные шаги:
- `Build release APK and AAB`
- `Upload release APK`
- `Upload release AAB`

Готовые артефакты появятся в разделе **Actions → Ваша сборка → Artifacts**.

### Важно

- **Никогда не коммитьте `*.jks` и `*.keystore` в репозиторий** — они уже исключены в `.gitignore`.
- Храните оригинальный `release.jks` в надёжном месте (локально, в менеджере паролей, в зашифрованном хранилище). Если он потерян, вы не сможете обновлять приложение в Google Play под тем же ключом.

## Ограничения

- Приложение содержит все статические файлы, включая MP3 и PDF, поэтому размер APK около **430 MB**.
- Service Worker из оригинальной PWA не нужен, так как файлы уже находятся локально.
- Некоторые динамические функции Next.js, зависящие от серверной части (например, `/api/progress`), не работают. Прогресс можно хранить через `localStorage`/`IndexedDB` WebView.

## Оффлайн

После установки приложение работает полностью офлайн: все уроки, квизы, аудио и PDF уже внутри APK.
