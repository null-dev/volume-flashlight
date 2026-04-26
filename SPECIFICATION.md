# volume-flashlight

volume-flashlight uses Shizuku to listen to hardware button evdev events on an Android device. Specifically, it detects when volume-down is long-pressed. When this happens, it toggles the flashlight.

The app should be as light on resources & battery as possible.

---

## Architecture

### Overview

The app is split across two processes:

1. **App process** — UI, flashlight control, lifecycle management.
2. **Shizuku UserService process** — privileged process that reads raw evdev events from `/dev/input/`.

The privileged process blocks on a `read()` syscall, so there is no polling and no wasted CPU. Communication back to the app process uses a Binder callback.

---

### Tech Stack

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Build:** Gradle with Kotlin DSL
- **Dependencies:**
  - `dev.rikka.shizuku:api` + `dev.rikka.shizuku:provider` — Shizuku integration
  - No other runtime dependencies

---

### Module / Package Structure

```
app/src/main/
├── aidl/com/nulldev/volumeflashlight/
│   ├── IInputEventService.aidl      # Privileged service interface
│   └── IInputEventCallback.aidl     # Callback into app process
├── java/com/nulldev/volumeflashlight/
│   ├── MainActivity.kt              # Minimal UI: status + enable/disable toggle
│   ├── FlashlightService.kt         # Foreground service: owns Shizuku binding & flashlight
│   ├── FlashlightManager.kt         # Thin wrapper around CameraManager.setTorchMode()
│   └── shizuku/
│       └── InputEventUserService.kt # Runs in Shizuku process; reads evdev
└── res/
    └── layout/activity_main.xml
```

---

### Component Details

#### `IInputEventService.aidl`

Interface implemented by `InputEventUserService` (privileged side):

```aidl
interface IInputEventService {
    void startListening(IInputEventCallback callback);
    void stopListening();
    void destroy();
}
```

#### `IInputEventCallback.aidl`

Callback implemented by `FlashlightService` (app side):

```aidl
interface IInputEventCallback {
    void onVolumeLongPress();
}
```

#### `InputEventUserService` (Shizuku process)

- Runs as a Shizuku `UserService` (persistent privileged process, not a one-shot command).
- On `startListening()`:
  1. Scans `/dev/input/event*` to find the device that reports volume button events (checks `/dev/input/by-path/` symlinks or tries each device node by reading its name via `/proc/bus/input/devices`).
  2. Opens that device file and spawns a dedicated reader thread.
  3. Reader thread blocks on `read()` for `input_event` structs (type=`EV_KEY`, code=114, value=1 for press / 0 for release).
  4. Tracks press timestamp; if released after ≥ 500 ms, fires `callback.onVolumeLongPress()`.
- On `stopListening()`: closes the fd, interrupts the reader thread.
- The process stays alive across app restarts and is reused if already running.

Long-press threshold is defined as a constant (default 500 ms) in this class.

> Note: The evdev read happens alongside normal Android input dispatch — the volume key still changes volume. Pure listening requires no event suppression.

#### `FlashlightService` (Foreground Service)

- Started by `MainActivity` when the user enables the feature.
- Posts a persistent notification (required for foreground services).
- Binds to Shizuku and, once bound, calls `IInputEventService.startListening(callback)`.
- Implements `IInputEventCallback.Stub`; on `onVolumeLongPress()`, delegates to `FlashlightManager.toggle()`.
- On service stop: calls `stopListening()` and unbinds.

#### `FlashlightManager`

- Holds a `CameraManager` reference.
- `toggle()`: checks current torch state and calls `setTorchMode(cameraId, enabled)`.
- Registers `CameraManager.TorchCallback` to track external torch state changes (e.g., another app turned off the torch).

#### `MainActivity`

- Shows current state: Shizuku available/unavailable, service running/stopped.
- Single toggle button: starts/stops `FlashlightService`.
- Requests Shizuku permission if not yet granted.
- No persistent logic lives here; it can be destroyed at any time.

---

### Lifecycle & Resource Usage

| Scenario | Behavior |
|---|---|
| Screen on/off | Service and UserService unaffected — no wakelock needed since evdev read is passive |
| App process killed | Shizuku UserService stays alive; reconnects on next service start |
| Shizuku not running | `FlashlightService` fails to bind and posts a user-visible error notification |
| Device rebooted | Shizuku must be re-activated by user; service is not auto-started |

The service does **not** hold a wakelock. The evdev reader thread inside the Shizuku process is idle (blocked in kernel) between events, so the impact on battery is negligible.

---

### Permissions

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.CAMERA" />  <!-- required for setTorchMode -->
```

Shizuku permission is requested at runtime via `Shizuku.requestPermission()`.

