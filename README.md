# volume-flashlight

Toggle your camera flashlight by holding the volume-down button — without root.

## How it works

Uses [Shizuku](https://shizuku.rikka.app/) to read raw evdev events from `/dev/input/` as the shell user. When volume-down is held for 500 ms, the flashlight toggles and the phone vibrates. The volume level is continuously restored while the button is held so that no unintended volume change occurs.

## Requirements

- Android 8.0+
- [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) running with permission granted

## Features

- **Long press** volume-down to toggle flashlight (500 ms threshold)
- **Haptic feedback** on toggle
- **Volume protection** — level is saved on press and restored continuously until release
- **Auto-detect** the volume button input device via `/proc/bus/input/devices`
- **Manual device picker** for phones where auto-detect fails
- **Start on boot** option
- **Evdev monitor** — live view of all input events with per-device filtering, useful for diagnosing which device to select

## Setup

1. Install and start Shizuku.
2. Install the app and open it.
3. Tap **Start** — Shizuku will prompt for permission if needed.
4. Optionally enable **Start on boot**.

If the flashlight doesn't respond, open the **Evdev Monitor**, hold the volume-down button, and note which device fires `KEY_VOLUMEDOWN` events. Then use **Pick input device** to select it manually.
