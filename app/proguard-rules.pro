# Keep AIDL-generated Binder classes
-keep class com.nulldev.volumeflashlight.IInputEventService { *; }
-keep class com.nulldev.volumeflashlight.IInputEventService$* { *; }
-keep class com.nulldev.volumeflashlight.IInputEventCallback { *; }
-keep class com.nulldev.volumeflashlight.IInputEventCallback$* { *; }

# Keep UserService — Shizuku loads it by class name
-keep class com.nulldev.volumeflashlight.shizuku.InputEventUserService { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
