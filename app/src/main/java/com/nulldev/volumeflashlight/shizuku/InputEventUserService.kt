package com.nulldev.volumeflashlight.shizuku

import android.os.Process
import android.util.Log
import com.nulldev.volumeflashlight.IEvdevEventCallback
import com.nulldev.volumeflashlight.IInputEventCallback
import com.nulldev.volumeflashlight.IInputEventService
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs inside the Shizuku privileged process (as shell user).
 * Opens the evdev device for the volume buttons and fires [IInputEventCallback.onVolumeLongPress]
 * when volume-down is held for at least [LONG_PRESS_MS] milliseconds.
 *
 * Reading is done on a dedicated thread that blocks on read() — no polling, no wakelock.
 */
class InputEventUserService : IInputEventService.Stub() {

    @Volatile private var running = false
    @Volatile private var inputStream: FileInputStream? = null
    private var readerThread: Thread? = null

    @Volatile private var monitorRunning = false
    private val monitorStreams = mutableListOf<FileInputStream>()
    private val monitorThreads = mutableListOf<Thread>()

    override fun startListening(callback: IInputEventCallback?) {
        // Replace any existing listener.
        stopListening()

        val devicePath = findVolumeDownDevice() ?: run {
            Log.e(TAG, "No volume-down device found; startListening aborted")
            return
        }

        running = true
        readerThread = Thread({ runReaderLoop(devicePath, callback) }, "evdev-reader").also {
            it.isDaemon = true
            it.start()
        }
    }

    override fun startListeningOnDevice(devicePath: String, callback: IInputEventCallback?) {
        stopListening()
        Log.i(TAG, "Starting listener on manually selected device: $devicePath")
        running = true
        readerThread = Thread({ runReaderLoop(devicePath, callback) }, "evdev-reader").also {
            it.isDaemon = true
            it.start()
        }
    }

    override fun stopListening() {
        running = false
        // Closing the stream causes the blocking read() to throw IOException, unblocking the thread.
        try { inputStream?.close() } catch (_: IOException) {}
        readerThread?.join(2_000)
        readerThread = null
    }

    override fun startMonitoring(callback: IEvdevEventCallback?) {
        stopMonitoring()
        monitorRunning = true
        val devices = File("/dev/input").listFiles { f -> f.name.startsWith("event") } ?: return
        synchronized(monitorStreams) {
            for (device in devices) {
                val fis = try { FileInputStream(device) } catch (_: IOException) { continue }
                monitorStreams.add(fis)
                val t = Thread({ runMonitorLoop(device.absolutePath, fis, callback) }, "evdev-mon-${device.name}")
                t.isDaemon = true
                t.start()
                monitorThreads.add(t)
            }
        }
    }

    override fun stopMonitoring() {
        monitorRunning = false
        synchronized(monitorStreams) {
            for (fis in monitorStreams) try { fis.close() } catch (_: IOException) {}
            monitorStreams.clear()
        }
        for (t in monitorThreads) t.join(2_000)
        monitorThreads.clear()
    }

    override fun destroy() {
        stopListening()
        stopMonitoring()
    }

    // ── evdev reader ──────────────────────────────────────────────────────────

    private fun runReaderLoop(devicePath: String, callback: IInputEventCallback?) {
        // input_event struct sizes:
        //   64-bit: timeval(16) + type(2) + code(2) + value(4) = 24 bytes
        //   32-bit: timeval(8)  + type(2) + code(2) + value(4) = 16 bytes
        val is64bit = Process.is64Bit()
        val timevalSize = if (is64bit) 16 else 8
        val eventSize  = timevalSize + 8 // + type(2) + code(2) + value(4)

        val fis = try {
            FileInputStream(devicePath)
        } catch (_: IOException) {
            return
        }
        inputStream = fis

        val buffer = ByteArray(eventSize)
        var pressTime = 0L

        try {
            while (running) {
                // Read exactly one input_event struct (kernel guarantees whole structs).
                var offset = 0
                while (offset < eventSize) {
                    val n = fis.read(buffer, offset, eventSize - offset)
                    if (n < 0) return
                    offset += n
                }

                val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                bb.position(timevalSize) // skip timeval
                val type  = bb.short.toInt() and 0xFFFF
                val code  = bb.short.toInt() and 0xFFFF
                val value = bb.int

                if (type == EV_KEY && code == KEY_VOLUMEDOWN) {
                    when (value) {
                        1 -> pressTime = System.currentTimeMillis() // key down
                        0 -> { // key up
                            val held = System.currentTimeMillis() - pressTime
                            if (pressTime > 0 && held >= LONG_PRESS_MS) {
                                try { callback?.onVolumeLongPress() } catch (_: Exception) {}
                            }
                            pressTime = 0
                        }
                        // value == 2 is key-repeat; ignore for long-press detection
                    }
                }
            }
        } catch (_: IOException) {
            // Stream closed or device error — expected shutdown path.
        } finally {
            try { fis.close() } catch (_: IOException) {}
            inputStream = null
        }
    }

    private fun runMonitorLoop(devicePath: String, fis: FileInputStream, callback: IEvdevEventCallback?) {
        val is64bit = Process.is64Bit()
        val timevalSize = if (is64bit) 16 else 8
        val eventSize = timevalSize + 8
        val buffer = ByteArray(eventSize)
        try {
            while (monitorRunning) {
                var offset = 0
                while (offset < eventSize) {
                    val n = fis.read(buffer, offset, eventSize - offset)
                    if (n < 0) return
                    offset += n
                }
                val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                bb.position(timevalSize)
                val type  = bb.short.toInt() and 0xFFFF
                val code  = bb.short.toInt() and 0xFFFF
                val value = bb.int
                try { callback?.onEvent(devicePath, type, code, value) } catch (_: Exception) {}
            }
        } catch (_: IOException) {
            // stream closed — expected shutdown path
        }
    }

    // ── device discovery ──────────────────────────────────────────────────────

    /**
     * Parses /proc/bus/input/devices to find the event device whose KEY bitmap
     * includes KEY_VOLUMEDOWN (bit 114).
     */
    private fun findVolumeDownDevice(): String? {
        return try {
            val content = File("/proc/bus/input/devices").readText()
            for (block in content.split(Regex("\n\n+"))) {
                val lines = block.lines()
                val handlersLine = lines.firstOrNull { it.startsWith("H: Handlers=") } ?: continue
                val keyLine      = lines.firstOrNull { it.startsWith("B: KEY=") }      ?: continue
                val bitmap       = keyLine.substringAfter("B: KEY=").trim()
                if (isKeyBitSet(bitmap, KEY_VOLUMEDOWN)) {
                    val m = Regex("event(\\d+)").find(handlersLine) ?: continue
                    val path = "/dev/input/event${m.groupValues[1]}"
                    Log.i(TAG, "Found volume-down device: $path")
                    return path
                }
            }
            null
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Checks whether [bit] is set in the space-separated hex bitmap printed by the kernel.
     * Words are listed most-significant first (big-endian word order), each word is 32 bits.
     */
    private fun isKeyBitSet(bitmap: String, bit: Int): Boolean {
        val words = bitmap.trim().split(Regex("\\s+")).reversed() // reverse → LSB word first
        val wordIndex = bit / 32
        val bitIndex  = bit % 32
        if (wordIndex >= words.size) return false
        val word = words[wordIndex].toLongOrNull(16) ?: return false
        return (word and (1L shl bitIndex)) != 0L
    }

    companion object {
        private const val TAG            = "InputEventUserService"
        private const val EV_KEY        = 1
        private const val KEY_VOLUMEDOWN = 114
        private const val LONG_PRESS_MS  = 500L
    }
}
