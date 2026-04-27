package com.nulldev.volumeflashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * Thin wrapper around [CameraManager.setTorchMode].
 * Tracks external torch state changes via [CameraManager.TorchCallback] so that
 * [toggle] always reflects the actual current state.
 */
class FlashlightManager(context: Context) {

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @Volatile private var torchEnabled = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchEnabled = enabled
        }
    }

    init {
        cameraManager.registerTorchCallback(torchCallback, null)
    }

    fun toggle() {
        val cameraId = flashCameraId ?: return
        try {
            cameraManager.setTorchMode(cameraId, !torchEnabled)
        } catch (_: Exception) {}
    }

    fun release() {
        cameraManager.unregisterTorchCallback(torchCallback)
    }

    private val flashCameraId: String? by lazy {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }
}
