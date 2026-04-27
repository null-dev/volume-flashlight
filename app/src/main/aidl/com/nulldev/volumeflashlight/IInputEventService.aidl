package com.nulldev.volumeflashlight;

import com.nulldev.volumeflashlight.IInputEventCallback;
import com.nulldev.volumeflashlight.IEvdevEventCallback;

// Interface exposed by the Shizuku UserService (privileged process).
interface IInputEventService {
    void startListening(IInputEventCallback callback);
    void stopListening();
    void startMonitoring(IEvdevEventCallback callback);
    void stopMonitoring();
    void destroy();
}
