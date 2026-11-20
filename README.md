# GIS Field Asset Management

Android app (Kotlin, Jetpack Compose, Google Maps) backed by a Node/Express REST API and a SQLite database. Field staff view utility assets (poles, transformers, substations) on a map, add new ones with a long-press, and switch their status between Active and Inactive.

> **AI disclosure:** this project was built with Claude Code (AI coding assistant), as permitted by the assessment brief.

## Architecture

```
Compose UI ──► AssetsViewModel (StateFlow) ──► AssetRepository ──► Retrofit/OkHttp ──► Express ──► SQLite
```

| Layer | Choice | Why |
|---|---|---|
| Android UI | Jetpack Compose + Material 3 | Declarative UI; map via `maps-compose` |
| State | MVVM, `ViewModel` + `StateFlow`, one-shot events via `Channel` | Single source of truth for map, list, sheets |
| Networking | Retrofit + OkHttp + kotlinx.serialization | Typed API, small footprint |
| Backend | Express 5 | Minimal REST framework |
| Database | SQLite via `better-sqlite3` | Persistent, zero setup, synchronous API suits a small service |

```
.
├── android/                 Android project (Gradle, Kotlin DSL, version catalog)
│   └── app/src/main/java/com/fieldassets/
│       ├── data/remote/         Retrofit API, DTOs, OkHttp setup
│       ├── data/repository/     AssetRepository (maps HTTP/IO errors to ApiException)
│       ├── domain/model/        Asset, AssetType, AssetStatus, NewAsset, ApiException
│       ├── presentation/map/    MapScreen, AssetsViewModel, UI state/events
│       ├── presentation/assets/ list panel, detail panel, add-asset sheet
│       ├── presentation/components/  shared chips, badges, segmented control
│       └── ui/theme/
├── backend/
│   ├── src/config|database|routes|controllers|services|middleware|validators
│   ├── src/database/schema.sql, seed-data.json, seed.js
│   ├── test/api.test.js     automated API tests (node:test)
│   └── data/                SQLite file is created here (git-ignored)
└── docs/screenshots/
```

## Prerequisites

- Node.js 18+ (developed on 23) and npm
- JDK 17, Android SDK with platform 35 (Android Studio recommended)
- A Google Maps SDK for Android API key (see below)
- An emulator image with Google Play services (the Maps SDK requires it)

## Backend

```bash
cd backend
npm install
cp .env.example .env     # optional; defaults: PORT=3000, HOST=0.0.0.0
npm start                # http://localhost:3000
npm test                 # 8 API tests against an in-memory database
npm run seed             # optional: seed an empty database explicitly
```

The database file is `backend/data/assets.db` (override with `DB_PATH`). The schema is applied on startup (`CREATE TABLE IF NOT EXISTS`) and the three sample assets are inserted **only if the table is empty**, so restarts never duplicate seed data or overwrite edits.

## Android

1. Create `android/local.properties` (git-ignored; see `local.properties.example`):

   ```properties
   sdk.dir=/path/to/Android/sdk
   MAPS_API_KEY=your-google-maps-android-api-key
   ```

   `MAPS_API_KEY` can alternatively be an environment variable. In Google Cloud Console enable **Maps SDK for Android** and (optionally) restrict the key to package `com.fieldassets` with your debug SHA-1 (`./gradlew signingReport`).
2. Start the backend, then build and run:

   ```bash
   cd android
   ./gradlew installDebug        # or open android/ in Android Studio and press Run
   ```

### Networking

- The default API base URL is `http://10.0.2.2:3000/`: `10.0.2.2` is the emulator's alias for the host machine's `localhost`.
- Physical device: put the machine's LAN IP in `android/local.properties`, e.g. `API_BASE_URL=http://192.168.1.20:3000/`, and make sure the phone and computer share a network. (`adb reverse tcp:3000 tcp:3000` with `API_BASE_URL=http://localhost:3000/` also works over USB.)
- Cleartext HTTP is permitted **only in the debug build** (`app/src/debug/AndroidManifest.xml`). Release builds are HTTPS-only.

## Using the app

- **Browse:** pan/zoom the map; markers are green (Active) or orange (Inactive). The bottom sheet lists all assets; drag it up to see more.
- **Inspect:** tap a marker or list row. The map animates to the asset and a detail sheet shows name, ID, type, coordinates and status.
- **Update status:** choose Active/Inactive, tap *Save status*. A spinner shows while saving; on failure the displayed status stays at the persisted value and an error is shown.
- **Add asset:** long-press the map (or use the *Add asset* button, which uses the map centre). Enter a name, choose type and initial status, submit. Validation errors from the API are shown inline. On success the list refreshes, the map centres on the new asset and its details open.
- Map controls: zoom in/out and *show all assets*. The refresh button in the top bar reloads from the API.

## API

Base path `/api/assets`. JSON bodies, WGS 84 coordinates (`latitude` −90…90, `longitude` −180…180).

| Method | Endpoint | Success | Errors |
|---|---|---|---|
| GET | `/api/assets` | 200 array of assets | 500 |
| POST | `/api/assets` | 201 created asset | 400 validation / invalid JSON, 409 duplicate id, 500 |
| PATCH | `/api/assets/:id` | 200 updated asset | 400 validation, 404 unknown id, 500 |
| GET | `/health` | 200 `{"status":"ok"}` | |

Asset fields: `id`, `name`, `asset_type` (`Pole` \| `Transformer` \| `Substation`), `latitude`, `longitude`, `status` (`Active` \| `Inactive`). On POST, `status` defaults to `Active` and `id` is generated (`P001`, `T002`, …) unless supplied.

All errors share one shape:

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "Request validation failed",
             "details": [ { "field": "latitude", "message": "latitude must be a number between -90 and 90" } ] } }
```

Codes: `VALIDATION_ERROR`, `INVALID_JSON`, `NOT_FOUND`, `DUPLICATE_ID`, `INTERNAL_ERROR`.

```bash
curl localhost:3000/api/assets

curl -X POST localhost:3000/api/assets -H 'Content-Type: application/json' \
  -d '{"name":"Field Pole","asset_type":"Pole","latitude":26.18,"longitude":91.78}'
# 201 {"id":"P002","name":"Field Pole","asset_type":"Pole","latitude":26.18,"longitude":91.78,"status":"Active"}

curl -X PATCH localhost:3000/api/assets/T001 -H 'Content-Type: application/json' -d '{"status":"Active"}'
# 200 {"id":"T001",...,"status":"Active"}

curl -X POST localhost:3000/api/assets -H 'Content-Type: application/json' \
  -d '{"name":"x","asset_type":"Pole","latitude":95,"longitude":-200}'
# 400 VALIDATION_ERROR with a details entry per bad field

curl -X PATCH localhost:3000/api/assets/NOPE -H 'Content-Type: application/json' -d '{"status":"Active"}'
# 404 NOT_FOUND
```

## Database

```sql
CREATE TABLE IF NOT EXISTS assets (
  id TEXT PRIMARY KEY, name TEXT NOT NULL, asset_type TEXT NOT NULL,
  latitude REAL NOT NULL, longitude REAL NOT NULL, status TEXT NOT NULL
);
```

Seed records live in `backend/src/database/seed-data.json` (P001, T001, S001). SQLite runs in WAL mode; data survives backend restarts. To reset, stop the server and delete `backend/data/assets.db*`.

## Verification status

Verified in this environment:

- **Backend:** 8 automated tests pass. Manually verified with curl against a running server: GET, POST, PATCH, missing fields, out-of-range coordinates, invalid status, nonexistent id, malformed JSON, and persistence of added/updated records across a backend restart (seed not re-applied).
- **Android:** `./gradlew assembleDebug` succeeds. Run on an API 35 emulator against the live backend: assets load from `GET /api/assets`; list and detail panels show the data; status change saved via PATCH and confirmed in the API; long-press opened the add sheet with captured coordinates; empty name shows a validation error; a valid submit created the asset through POST and opened its details; with the backend stopped, a status save shows an error and leaves the persisted status unchanged. Screenshots are in `docs/screenshots/`.

**Not verified:** map tiles, marker rendering and camera animation. No Google Maps API key was supplied, so the screenshots show the map's blank unauthorised state (and long-press coordinates there come from the SDK's default camera, not Guwahati). Marker colours, marker taps and camera moves are implemented but need a key to be seen. Add `MAPS_API_KEY` and re-run to confirm.

## Known limitations

- Requires a Maps API key; without it the map is blank (the rest of the app still works).
- No authentication, offline support, pagination, delete/edit-details endpoints (not required by the brief).
- `PATCH` only updates `status`.
- Markers use Google's default hue-tinted pins rather than per-type icons.
- Android has no automated tests; the backend has API tests.
- Compose is pinned to library versions compatible with `compileSdk 35` (`androidx.core` 1.16, `activity` 1.10.1).
