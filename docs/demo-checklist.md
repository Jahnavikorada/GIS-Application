# Demo recording checklist

Setup: backend running (`npm start`), emulator on API 35 with Google Play, `MAPS_API_KEY` set, app installed.

1. Show `curl localhost:3000/api/assets` returning the 3 seeded assets.
2. Launch the app: map loads, three markers visible (two green, one orange), count badge shows 3.
3. Tap a marker: detail sheet shows name, ID, type, coordinates, status.
4. Tap a list row: map animates to that asset.
5. Switch an asset's status and tap *Save status*: spinner, snackbar, chip and marker colour update. Show the PATCH result via curl.
6. Long-press an empty spot: Add sheet shows captured coordinates.
7. Submit with an empty name: inline validation error.
8. Enter a name, pick a type, submit: new marker appears, map centres on it, details open.
9. Stop the backend; try saving a status: error snackbar, displayed status unchanged. Restart backend, tap refresh.
10. Restart the backend and the app: added asset and changed status are still there.
11. Show the invalid-coordinate curl returning 400 with `VALIDATION_ERROR`.
