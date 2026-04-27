package com.nulldev.volumeflashlight

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.media.AudioManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.os.VibrationEffect
import android.os.Vibrator
import com.nulldev.volumeflashlight.shizuku.InputEventUserService
import rikka.shizuku.Shizuku

/**
 * Foreground service that owns the Shizuku UserService binding and the flashlight.
 *
 * Lifecycle:
 *  - Started by MainActivity after Shizuku permission is confirmed.
 *  - Binds to [InputEventUserService] (running in Shizuku's privileged process).
 *  - On [IInputEventCallback.onVolumeLongPress], toggles the torch.
 *  - On stop, unregisters the callback and destroys the UserService.
 */
class FlashlightService : Service() {

    private lateinit var flashlightManager: FlashlightManager
    private val vibrator by lazy { getSystemService(Vibrator::class.java) }
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

    @Volatile private var inputEventService: IInputEventService? = null
    @Volatile private var savedVolume = -1
    @Volatile private var restoring = false
    private var restoreThread: Thread? = null

    private val callback = object : IInputEventCallback.Stub() {
        override fun onVolumeKeyDown() {
            savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }

        override fun onVolumeKeyUp() {
            restoring = false
            restoreThread?.interrupt()
            restoreThread = null
        }

        override fun onVolumeLongPress() {
            flashlightManager.toggle()
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            // Continuously restore the saved volume until the key is released, so that
            // the system's auto-repeat volume adjustment is kept at bay.
            restoring = true
            restoreThread = Thread {
                val vol = savedVolume
                while (restoring && vol >= 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                    try { Thread.sleep(50) } catch (_: InterruptedException) { break }
                }
            }.also { it.isDaemon = true; it.start() }
        }
    }

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(packageName, InputEventUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("input_event")
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = IInputEventService.Stub.asInterface(binder) ?: return
            inputEventService = service
            try {
                val savedDevice = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString(PREF_INPUT_DEVICE, null)
                if (savedDevice != null) {
                    service.startListeningOnDevice(savedDevice, callback)
                } else {
                    service.startListening(callback)
                }
            } catch (_: RemoteException) {
                stopSelf()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            inputEventService = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        flashlightManager = FlashlightManager(this)
        startForegroundWithNotification()
        connectShizuku()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { inputEventService?.stopListening() } catch (_: RemoteException) {}
        try { Shizuku.unbindUserService(userServiceArgs, serviceConnection, true) } catch (_: Exception) {}
        flashlightManager.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── private helpers ───────────────────────────────────────────────────────

    private fun startForegroundWithNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Flashlight Service", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun connectShizuku() {
        if (!Shizuku.pingBinder()) {
            stopSelf()
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    companion object {
        @Volatile var isRunning = false

        const val PREFS_NAME       = "app_prefs"
        const val PREF_INPUT_DEVICE = "selected_input_device"

        private const val CHANNEL_ID      = "flashlight_service"
        private const val NOTIFICATION_ID = 1
    }
}
