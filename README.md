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
./gradlew :composeApp:packageDistributionForCurrentOS
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
