# InspeKt

A **Kotlin Multiplatform** (KMP) REST API client for **Desktop** (Linux/macOS/Windows) and **Android**, built with Compose Multiplatform. Inspired by Postman.

## Features

- **Test REST APIs** — GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
- **Query params, headers, body** (JSON, form-urlencoded, multipart, raw text)
- **Collections** — Create, edit, and share API collections in **Postman v2.1 JSON** format
- **Import / Export** — Drop any Postman JSON collection in or export one out
- **Import from cURL** — Paste a `curl` command and it becomes a request instantly

## Architecture

```
composeApp/
├── commonMain/
│   ├── data/
│   │   ├── model/          # Postman JSON DTOs
│   │   └── repository/     # HttpClientRepository, CollectionRepository, PostmanSerializer
│   ├── domain/
│   │   └── model/          # HttpRequest, HttpResponse, Collection, KeyValueParam, ...
│   ├── presentation/
│   │   ├── components/     # Shared Compose components (KeyValueRow, CodeEditor, badges)
│   │   ├── screens/        # RequestScreen, CollectionsPanel, ResponsePanel
│   │   ├── viewmodel/      # RequestViewModel, CollectionsViewModel
│   │   └── navigation/     # AppDestination
│   ├── di/                 # Koin module
│   └── util/               # CurlParser
├── androidMain/            # Android entry point (MainActivity, InspeKtApp)
└── desktopMain/            # Desktop entry point (Main.kt)
```

## Tech Stack

| Library | Purpose |
|---|---|
| Compose Multiplatform 1.7 | UI across platforms |
| Ktor 3.0 (OkHttp / Java) | HTTP client |
| Kotlinx Serialization 1.7 | JSON (de)serialization |
| Koin 4.0 | Dependency injection |
| Okio 3.9 | File I/O for collection persistence |
| Kotlin Coroutines 1.9 | Async request execution |

## Getting Started

### Prerequisites

- JDK 17+
- Android SDK (for Android target)

### Run Desktop

```bash
./gradlew :composeApp:run
```

### Build Android APK

```bash
./gradlew :composeApp:assembleDebug
```

### Build desktop distribution

```bash
# Build installer for the current OS (auto-detects platform)
./gradlew :composeApp:packageDistributionForCurrentOS
```

#### Platform-specific installers

| Platform | Format | Command |
|---|---|---|
| **Linux** | `.deb` (Debian/Ubuntu) | `./gradlew :composeApp:packageDeb` |
| **Linux** | `.rpm` (Fedora/RHEL) | `./gradlew :composeApp:packageRpm` |
| **macOS** | `.dmg` | `./gradlew :composeApp:packageDmg` |
| **macOS** | `.pkg` | `./gradlew :composeApp:packagePkg` |
| **Windows** | `.msi` | `./gradlew :composeApp:packageMsi` |
| **Windows** | `.exe` | `./gradlew :composeApp:packageExe` |

> **Cross-compilation is not supported** — each installer can only be built on
> its native OS (e.g. `.msi`/`.exe` on Windows, `.dmg` on macOS). A **GitHub
> Actions CI workflow** (`.github/workflows/build-installers.yml`) builds all
> platforms in parallel so you don't need access to every OS yourself.

Built installers are output to: `composeApp/build/compose/binaries/main/`

#### CI / GitHub Actions

All installers are built automatically when you push a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This triggers the workflow which:
1. Builds `.deb` and `.rpm` on Linux, `.dmg` on macOS, `.msi` and `.exe` on Windows — all in parallel
2. Uploads every installer as a GitHub Actions artifact
3. Creates a **GitHub Release** with all installers attached

You can also trigger the workflow manually from the **Actions** tab.

#### Release build (with ProGuard optimization)

```bash
./gradlew :composeApp:packageReleaseDistributionForCurrentOS
```

#### JDK requirement for packaging

Packaging requires a full JDK 17+ with `jpackage`. Android Studio's bundled
JBR may not include it. Set `JPACKAGE_JDK` to point to a full JDK:

```bash
export JPACKAGE_JDK=/usr/lib/jvm/java-21-openjdk-amd64   # Linux example
./gradlew :composeApp:packageDeb
```

## Collections Storage

Collections are stored as standard **Postman Collection v2.1 JSON** files in:
- **Desktop:** `~/.inspekt/collections/`
- **Android:** app internal storage `/data/data/com.inspekt/files/collections/`

You can share these files directly with Postman or any compatible tool.

## cURL Import Examples

```bash
# Simple GET
curl https://api.github.com/users/octocat

# POST with JSON body
curl -X POST https://api.example.com/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer mytoken" \
  -d '{"name":"Alice","email":"alice@example.com"}'

# Form data
curl -X POST https://api.example.com/login \
  -d "username=alice&password=secret"
```
