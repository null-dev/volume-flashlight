package com.nulldev.volumeflashlight;

import com.nulldev.volumeflashlight.IInputEventCallback;

// Interface exposed by the Shizuku UserService (privileged process).
interface IInputEventService {
    void startListening(IInputEventCallback callback);
    void stopListening();
    void destroy();
}
