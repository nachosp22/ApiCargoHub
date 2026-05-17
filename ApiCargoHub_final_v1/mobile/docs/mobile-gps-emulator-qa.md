# Mobile GPS Emulator QA (Madrid)

Use `qa-madrid-tracking-route.gpx` to simulate driver movement in Madrid on Android Studio Emulator.

## Load GPX in Android Studio Emulator
1. Open emulator.
2. Click **Extended controls** (three dots).
3. Go to **Location**.
4. Open **Routes** (or **GPX/KML** depending on emulator version).
5. Click **Load GPX/KML** and select `mobile/docs/qa-madrid-tracking-route.gpx`.
6. Press **Play route**.

## What to verify
- Driver marker moves smoothly point-to-point.
- Bearing/flecha rotates with direction changes.
- Route polyline is drawn and stays synced with current position.
- Tracking/sync status updates while moving (online/offline or queued/sent states).
- Foreground service remains active during route playback (no unexpected stop).

## Notes
- Route is a short central Madrid urban loop (~12 points), good for quick QA cycles.
- If movement is too fast/slow, adjust emulator route playback speed.
