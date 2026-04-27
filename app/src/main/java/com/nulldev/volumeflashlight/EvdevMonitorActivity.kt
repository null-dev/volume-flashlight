package com.nulldev.volumeflashlight

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import com.nulldev.volumeflashlight.shizuku.InputEventUserService
import rikka.shizuku.Shizuku
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Shows a live tail of raw evdev events from all /dev/input/event* devices.
 * Monitoring starts in onResume and stops in onPause so no work is done in the background.
 */
class EvdevMonitorActivity : Activity() {

    private lateinit var tvLog: TextView
    private lateinit var scrollView: ScrollView

    private val handler = Handler(Looper.getMainLooper())

    private var inputEventService: IInputEventService? = null
    private var serviceBound = false
    private var paused = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(packageName, InputEventUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("input_event")
            .version(BuildConfig.VERSION_CODE)
    }

    // Binder threads write here; main thread drains it on a fixed-rate tick.
    private val pendingLines = ConcurrentLinkedDeque<String>()
    private val logLines = ArrayDeque<String>(MAX_LINES + 1)

    private val evdevCallback = object : IEvdevEventCallback.Stub() {
        override fun onEvent(devicePath: String, type: Int, code: Int, value: Int) {
            val devName = devicePath.substringAfterLast("/")
            pendingLines.addLast("$devName  type=%-5d  code=%-5d  value=$value".format(type, code))
        }
    }

    // Drains pendingLines and redraws at most once per UI_INTERVAL_MS.
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            var changed = false
            while (pendingLines.isNotEmpty()) {
                logLines.addLast(pendingLines.pollFirst() ?: break)
                if (logLines.size > MAX_LINES) logLines.removeFirst()
                changed = true
            }
            if (changed) {
                tvLog.text = logLines.joinToString("\n")
                scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
            handler.postDelayed(this, UI_INTERVAL_MS)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = IInputEventService.Stub.asInterface(binder) ?: return
            inputEventService = svc
            try {
                svc.startMonitoring(evdevCallback)
            } catch (_: Exception) {}
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            inputEventService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evdev_monitor)
        tvLog = findViewById(R.id.tv_log)
        scrollView = findViewById(R.id.scroll_view)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setActionBar(toolbar)
        actionBar?.title = getString(R.string.evdev_monitor_title)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.mutate()?.setTint(Color.WHITE)

        window.statusBarColor = Color.BLACK
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_evdev_monitor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_pause -> {
            paused = !paused
            item.setIcon(if (paused) R.drawable.ic_resume else R.drawable.ic_pause)
            item.title = getString(if (paused) R.string.menu_resume else R.string.menu_pause)
            if (paused) handler.removeCallbacks(uiUpdateRunnable)
            else handler.post(uiUpdateRunnable)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (!paused) handler.post(uiUpdateRunnable)
        if (Shizuku.pingBinder()) {
            try {
                Shizuku.bindUserService(userServiceArgs, serviceConnection)
                serviceBound = true
            } catch (_: Exception) {
                tvLog.text = "Failed to bind Shizuku user service.\n"
            }
        } else {
            tvLog.text = "Shizuku is not running.\n"
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(uiUpdateRunnable)
        try { inputEventService?.stopMonitoring() } catch (_: Exception) {}
        inputEventService = null
        if (serviceBound) {
            try { Shizuku.unbindUserService(userServiceArgs, serviceConnection, false) } catch (_: Exception) {}
            serviceBound = false
        }
    }

    companion object {
        private const val MAX_LINES = 200
        private const val UI_INTERVAL_MS = 25L
    }
}
