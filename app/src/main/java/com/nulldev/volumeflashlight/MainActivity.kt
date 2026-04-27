package com.nulldev.volumeflashlight

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import rikka.shizuku.Shizuku

/**
 * Minimal UI: shows Shizuku/service status and a single Start/Stop toggle.
 * All persistent work lives in [FlashlightService]; this Activity can be destroyed freely.
 */
class MainActivity : Activity() {

    private lateinit var tvShizukuStatus: TextView
    private lateinit var tvServiceStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var btnEvdevMonitor: Button

    private val handler = Handler(Looper.getMainLooper())

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        updateUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvShizukuStatus = findViewById(R.id.tv_shizuku_status)
        tvServiceStatus = findViewById(R.id.tv_service_status)
        btnToggle       = findViewById(R.id.btn_toggle)
        btnEvdevMonitor = findViewById(R.id.btn_evdev_monitor)

        btnToggle.setOnClickListener { onToggleClicked() }
        btnEvdevMonitor.setOnClickListener {
            startActivity(Intent(this, EvdevMonitorActivity::class.java))
        }
        Shizuku.addRequestPermissionResultListener(permissionListener)
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun updateUi() {
        val shizukuRunning = Shizuku.pingBinder()
        val hasPermission  = shizukuRunning &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        val serviceRunning = FlashlightService.isRunning

        tvShizukuStatus.text = when {
            !shizukuRunning -> getString(R.string.shizuku_not_running)
            !hasPermission  -> getString(R.string.shizuku_no_permission)
            else            -> getString(R.string.shizuku_ready)
        }

        tvServiceStatus.text = if (serviceRunning)
            getString(R.string.service_active)
        else
            getString(R.string.service_inactive)

        // Button is only useful when Shizuku is reachable.
        btnToggle.isEnabled = shizukuRunning
        btnToggle.text = if (serviceRunning) getString(R.string.btn_stop)
                         else getString(R.string.btn_start)
    }

    private fun onToggleClicked() {
        if (!Shizuku.pingBinder()) return

        // If no permission yet, request it — the listener will call updateUi() on result.
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_CODE)
            return
        }

        val intent = Intent(this, FlashlightService::class.java)
        if (FlashlightService.isRunning) {
            stopService(intent)
        } else {
            startForegroundService(intent)
        }

        // Service state updates asynchronously; refresh after a short delay.
        handler.postDelayed(::updateUi, 300)
    }

    companion object {
        private const val SHIZUKU_PERMISSION_CODE = 100
    }
}
