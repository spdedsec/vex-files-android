# VEX Files

A lightweight Android-first file manager built around a deliberate rule: **filesystem work first, decoration last**.

It is intentionally local, fast, dark, sharp, and quiet. No AI layer. No account. No cloud dependency. No gradients. No floating-card UI.

## What is included

- Internal-storage browser with fast list rendering
- Folder navigation and breadcrumbs
- File open/share integration via `FileProvider`
- Copy, move, rename-ready architecture, delete, multi-select
- ZIP creation and ZIP extraction
- Search in the current directory
- Sort by name / size / date / type
- Recent folders/files
- Favorites for current folder
- Storage/free-space information
- Folder creation
- Limited Storage Access Framework fallback
- Manual system "All files access" entry point
- Very short 120 ms UI transitions instead of large motion sequences
- Native Kotlin + XML views; no Compose runtime
- Small dependency surface: AndroidX Core, AppCompat, RecyclerView, Activity

## UI direction

The visual system is based on Swiss editorial design and old-school utility software:

- near-black background
- warm paper white for primary text
- red as the functional accent
- monospaced labels for utility/navigation
- square geometry
- thin separators instead of card stacks
- dense information hierarchy
- actions represented as text rather than giant rounded controls
- dark mode by default

The project deliberately avoids the usual blue-purple gradient / glassmorphism / oversized-card treatment.

## Architecture

```text
app/src/main/java/com/vex/files/
├── core/
│   ├── FileItem.kt       # filesystem model wrappers
│   ├── FileOps.kt        # copy/move/delete/zip/extract
│   ├── FormatUtils.kt    # sizes, dates, MIME, storage stats
│   └── Prefs.kt          # recent/favorite persistence
└── ui/
    ├── MainActivity.kt   # app shell + navigation/actions
    └── FileAdapter.kt    # RecyclerView rows + selection
```

This is a deliberately small first release. The core filesystem operations are isolated so a future desktop client can reuse the same behavior behind another UI layer.

## Storage model

VEX Files is designed to feel like the primary file browser, so its full local mode uses Android's special **All files access** capability. Android documents `MANAGE_EXTERNAL_STORAGE` as the broad-storage route for file-management apps, while the Storage Access Framework remains available when the app needs user-selected directories. See the developer notes below for policy considerations.

The app does **not** request network access and has no server component.

## Build

### Requirements

- Android Studio compatible with AGP 9.4
- JDK 17
- Android SDK Platform 36
- Android 11 (API 30) or newer device
- Android Build Tools 36.0.0
- Internet access for the first Gradle dependency sync

The current project targets API 36. New Google Play submissions have targeted API-level requirements; as of August 31, 2026 new apps and app updates must target Android 16 / API 36 or higher.

### Android Studio

1. Open this directory as an existing Android Studio project.
2. Install SDK Platform 36 when prompted.
3. Let Gradle sync.
4. Connect an Android phone with Developer Options + USB debugging enabled, or start an emulator.
5. Run the `app` configuration.

### Command line

```bash
./gradlew assembleDebug
./gradlew installDebug
```

The supplied wrapper properties point to Gradle 9.6. If your Android Studio installation generates a newer compatible wrapper, it is safe to regenerate it.

### APK

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK:

```bash
./gradlew assembleRelease
```

For a public release, configure your own signing key and Play App Signing workflow. Do not commit keystores or signing passwords.

## Developer notes

### 1. Storage permission is deliberate

`MANAGE_EXTERNAL_STORAGE` is a special permission. On Android, it is appropriate for categories such as file managers, but Google Play has separate policy requirements around restricted permissions. A sideload/internal build can use the capability for the intended primary-file-manager experience. Before publishing to Play, review the current All files access policy and ensure the app's core functionality and declaration qualify.

The app opens the system settings page rather than trying to fake or automate the permission grant.

### 2. SAF fallback

`ACTION_OPEN_DOCUMENT_TREE` is used as the user-controlled fallback path. In a future production release, destination copy/move operations should be fully bridged into a URI-based provider abstraction so a user-selected SAF directory can be used without raw `File` paths.

### 3. Android 15/16 edge-to-edge

The target is API 36. Android 15 introduced enforced edge-to-edge behavior for apps targeting API 35+, and Android 16 removes the legacy opt-out path. The current UI applies transparent system bars and uses explicit top padding so content is not hidden behind the status bar.

### 4. Performance rules

- Directory scanning happens off the main thread.
- File operations happen off the main thread.
- RecyclerView is used for directory rows.
- No network initialization.
- No image-loading library.
- No large animation framework.
- No database for v0.1; recent/favorite paths are tiny SharedPreferences payloads.

### 5. ZIP security

Extraction validates each entry's canonical path and rejects path traversal outside the destination directory.

### 6. Intentional v0.1 limitations

- ZIP is the only archive format in the first cut.
- No recycle bin yet; delete is permanent.
- Batch rename rules are not included yet; single-item rename is included.
- Copy/move destinations are kept within the current directory tree in v0.1; a full URI-backed destination abstraction is planned for the next pass.
- Media thumbnails are intentionally omitted to keep the app small and filesystem navigation fast.
- No background indexing service.
- No cloud providers.

## Suggested next release

1. URI-backed SAF operations with `DocumentFile`
2. Full rename dialog + batch rename rules
3. Image/video lightweight thumbnails with strict memory limits
4. Duplicate finder using size + hash-on-demand
5. Storage analyzer
6. Archive browser without full extraction
7. Recycle-bin/trash abstraction where platform/vendor support allows
8. Proper multi-window/tablet layout
9. Accessibility pass for TalkBack and keyboard navigation
10. Release signing + Play policy declaration workflow

## Design tokens

```text
INK       #0A0A0A
PAPER     #F1F0EB
MUTED     #A7A49C
LINE      #2B2B29
RED       #E41E2B
RED-DARK  #A80F19
```

Keep the palette small. New colors should require a reason.

## License

This starter project is intentionally left without a license so the owner can choose one before publishing.
