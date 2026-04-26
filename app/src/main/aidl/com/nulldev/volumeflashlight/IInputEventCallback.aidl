package com.nulldev.volumeflashlight;

// Callback from the privileged Shizuku process back into the app process.
interface IInputEventCallback {
    void onVolumeLongPress();
}
