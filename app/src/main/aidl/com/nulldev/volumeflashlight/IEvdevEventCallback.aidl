package com.nulldev.volumeflashlight;

// Callback from the privileged Shizuku process into the app process for raw evdev events.
oneway interface IEvdevEventCallback {
    void onEvent(String devicePath, int type, int code, int value);
}
