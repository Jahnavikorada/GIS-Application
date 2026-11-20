# Handover and Knowledge Transfer

Project: **GIS Field Asset Management** (Android + REST API + SQLite), built for the Android Full Stack Developer practical assessment (3 hours, 100 marks).

Start with `README.md` for setup and API reference. This document explains **what exists, why it is built this way, what is and is not verified, and what to do next**.

## 1. Status at a glance

| Area | State |
|---|---|
| Backend API (GET / POST / PATCH, validation, error shape) | Done, 8 automated tests pass, manually verified with curl |
| SQLite persistence and seed | Done, verified across a restart (seed does not re-run) |
| Android app (list, detail, add, status update, loading/error states) | Done, builds, run on an API 35 emulator against the live backend |
| Map tiles, marker colours, marker tap, camera animation | **Written but not seen.** No Google Maps API key was available |
| Android automated tests | None (backend only) |
| Git | Repo initialised, **nothing committed yet** |

**The one thing left to do:** add a Google Maps API key and confirm the map and markers (section 4).

## 2. How to run it (on a new machine)

Prerequisites: Node.js 20 or 22 (LTS), JDK 17, Android Studio (or the Android SDK with platform 35), an emulator with **Google Play services** (e.g. "Medium Phone" API 35 with Play Store).

```bash
# 1. Backend
cd backend
npm install
npm start            # http://localhost:3000, creates backend/data/assets.db and seeds 3 assets
npm test             # optional, 8 tests

# 2. Android (new terminal)
cd android
# create android/local.properties (git-ignored), see local.properties.example:
#   sdk.dir=/path/to/Android/sdk
#   MAPS_API_KEY=your-key        <- needed to see the map
./gradlew installDebug           # or open android/ in Android Studio and press Run
```

If `npm install` fails building `better-sqlite3`, install the platform build tools (Xcode command line tools on macOS, `build-essential` on Linux, VS Build Tools on Windows) or use Node 20/22 where prebuilt binaries exist.

## 3. Code walkthrough (KT)

### Request flow

```
Compose UI -> AssetsViewModel (StateFlow state + Channel events) -> AssetRepository
           -> Retrofit/OkHttp -> Express route -> controller -> validator + service -> SQLite
```

### Backend (`backend/src`)

| File | Responsibility |
|---|---|
| `server.js` | Entry point: opens DB, seeds if empty, listens, graceful shutdown |
| `app.js` | Builds the Express app (used by both the server and the tests) |
| `config/index.js` | `PORT`, `HOST`, `DB_PATH` from env / `.env` |
| `database/db.js`, `schema.sql`, `seed-data.json`, `seed.js` | Schema applied on startup; seed only when the table is empty |
| `validators/assetValidator.js` | All input rules; returns a list of `{field, message}` |
| `services/assetService.js` | SQL via prepared statements; ID generation (`P001`, `T002`, ...) |
| `controllers/assetController.js` | HTTP status mapping (200/201/400/404/409) |
| `middleware/errors.js` | One JSON error shape, 404 handler, malformed-JSON handler, 500 handler |

Decisions worth being able to explain:
- **Strict validation:** coordinates must be JSON numbers (a string like `"91.8"` is rejected) and in range; all problems are returned together, not one at a time.
- **Seed only into an empty table** so restarts never duplicate data or overwrite edits.
- **Server-generated IDs** (prefix by type + next number). A client may pass an `id`; a duplicate returns 409.
- **`better-sqlite3`** (synchronous, simple, fast for this size). **Pinned to v12:** v13 segfaulted on the dev machine.
- **WAL mode** so reads and writes do not block each other.
- `createApp(db)` takes the DB as a parameter, so tests use an in-memory database.

### Android (`android/app/src/main/java/com/fieldassets`)

| Package | Contents |
|---|---|
| `domain/model` | `Asset`, `AssetType`, `AssetStatus`, `NewAsset`, `ApiException` (no Android or network types) |
| `data/remote` | `AssetApi` (Retrofit), DTOs with `@SerialName("asset_type")`, `NetworkModule` (OkHttp, timeouts, debug logging) |
| `data/repository` | `AssetRepository`: wraps calls in `Result`, converts HTTP error bodies into `ApiException` with per-field errors, maps IO failures to a friendly message, rethrows `CancellationException` |
| `presentation/map` | `MapScreen` (map, markers, sheets, controls), `AssetsViewModel`, `AssetsUiState`, `UiEvent` |
| `presentation/assets` | `AssetListPanel`, `AssetDetailPanel`, `AddAssetSheet` |
| `presentation/components` | `StatusChip`, `AssetTypeBadge`, segmented control, label/value row |
| `FieldAssetsApp` | Holds the repository (simple manual DI, no framework) |

Key behaviours to know:
- **State vs events:** persistent UI state is a `StateFlow<AssetsUiState>`; one-shot things (move camera, fit all, show snackbar) go through a `Channel<UiEvent>` so they are not replayed on rotation.
- **Status update:** the segmented control sets a *pending* status locally; *Save status* sends PATCH. On success the asset in state is replaced by the server's response. On failure the pending value is dropped, so the screen always shows the **persisted** status, plus an error snackbar.
- **Add asset:** long-press -> `startAdd(lat, lng)` -> bottom sheet. Name is validated locally; API field errors are mapped to the right input. On success: close the sheet, re-fetch the list (falls back to the created record if the refresh fails), select the new asset (this moves the camera) and show a snackbar.
- **Initial camera:** after the first successful load the map fits all markers (needs the map to be loaded, which needs a valid key).
- **Markers:** default Google pins tinted by hue, green = Active, orange = Inactive; selected marker has a higher z-index. The *Add asset* button uses the map centre.
- **Networking config:** `API_BASE_URL` and `MAPS_API_KEY` come from `android/local.properties` or env vars via `BuildConfig` and manifest placeholders. Default URL `http://10.0.2.2:3000/` (emulator alias for the host). Cleartext HTTP is allowed only in the **debug** manifest.

## 4. Remaining step: Google Maps API key

1. Google Cloud console -> create or choose a project (billing / card verification is required by Google; there is a no-charge free tier).
2. Enable **Maps SDK for Android**.
3. Keys & Credentials -> Create API key. Optionally restrict it to package `com.fieldassets` and your debug SHA-1 (`cd android && ./gradlew signingReport`).
4. Put it in `android/local.properties` as `MAPS_API_KEY=...`. **Never commit it** (the file is git-ignored).
5. Rebuild, run, and confirm: tiles load, three markers appear (two green, one orange), tapping a marker opens its details, tapping a list row animates the camera, long-press captures coordinates near Guwahati (26.16, 91.76).

If the map is grey with no tiles, check `adb logcat | grep "Maps SDK"`; an authorization error means the key, API enablement or package/SHA-1 restriction is wrong.

## 5. Known gotchas

- **Blank map without a key.** The rest of the app still works. The long-press coordinates in the saved screenshots (Seattle) come from the SDK's default camera in that unauthorised state, not from a bug in capture logic.
- **Dependency pins:** `androidx.core` 1.16.0 and `androidx.activity` 1.10.1 because newer versions require `compileSdk 36`; this machine has SDK 35.
- **`better-sqlite3` pinned to 12.x** (13.x crashed).
- **Node 23 test runner:** the `npm test` script uses a quoted glob (`"test/*.test.js"`) because passing the directory fails.
- **Physical device:** set `API_BASE_URL` to the computer's LAN IP, or `adb reverse tcp:3000 tcp:3000` with `API_BASE_URL=http://localhost:3000/`.
- **Resetting data:** stop the backend, delete `backend/data/assets.db*`, start again to reseed.

## 6. Suggested next steps / extras

1. Add the Maps key and verify (section 4); re-take screenshots with real tiles for the submission.
2. Record the short demo using `docs/demo-checklist.md`.
3. Make the first commit (suggested message: `Add GIS field asset management app: Compose/Maps Android client and Express/SQLite API`).
4. Nice-to-haves if time allows: ViewModel unit tests (fake repository + `kotlinx-coroutines-test`, already a dependency), per-type marker icons, a `DELETE` endpoint.

## 7. Disclosure

The assessment allows AI assistance if disclosed. This project was built with Claude Code and the README states so. Be prepared to explain the design choices above in your own words.

## 8. What to share

Share the single ZIP `gis-field-assets.zip`. It contains source, docs, screenshots, Gradle wrapper and lockfile, and **excludes** `node_modules`, `android/build`, `.gradle`, `local.properties`, the SQLite database and `.git`. It contains **no secrets**; the recipient creates their own `local.properties`. Do **not** share a `local.properties` that contains a real `MAPS_API_KEY`.
