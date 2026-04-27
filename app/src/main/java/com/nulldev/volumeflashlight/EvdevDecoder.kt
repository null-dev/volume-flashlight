package com.nulldev.volumeflashlight

/**
 * Decodes raw Linux input_event (type, code, value) tuples into human-readable strings.
 * Constants sourced from linux/input-event-codes.h.
 */
object EvdevDecoder {

    // ── Event types ──────────────────────────────────────────────────────────

    private val EVENT_TYPES = mapOf(
        0x00 to "EV_SYN",
        0x01 to "EV_KEY",
        0x02 to "EV_REL",
        0x03 to "EV_ABS",
        0x04 to "EV_MSC",
        0x05 to "EV_SW",
        0x11 to "EV_LED",
        0x12 to "EV_SND",
        0x14 to "EV_REP",
        0x15 to "EV_FF",
        0x16 to "EV_PWR",
        0x17 to "EV_FF_STATUS",
    )

    // ── EV_SYN codes ────────────────────────────────────────────────────────

    private val SYN_CODES = mapOf(
        0 to "SYN_REPORT",
        1 to "SYN_CONFIG",
        2 to "SYN_MT_REPORT",
        3 to "SYN_DROPPED",
        0xf to "SYN_MAX",
    )

    // ── EV_KEY codes ─────────────────────────────────────────────────────────

    private val KEY_CODES = mapOf(
        0 to "KEY_RESERVED",
        1 to "KEY_ESC",
        2 to "KEY_1", 3 to "KEY_2", 4 to "KEY_3", 5 to "KEY_4", 6 to "KEY_5",
        7 to "KEY_6", 8 to "KEY_7", 9 to "KEY_8", 10 to "KEY_9", 11 to "KEY_0",
        12 to "KEY_MINUS", 13 to "KEY_EQUAL",
        14 to "KEY_BACKSPACE", 15 to "KEY_TAB",
        16 to "KEY_Q", 17 to "KEY_W", 18 to "KEY_E", 19 to "KEY_R", 20 to "KEY_T",
        21 to "KEY_Y", 22 to "KEY_U", 23 to "KEY_I", 24 to "KEY_O", 25 to "KEY_P",
        26 to "KEY_LEFTBRACE", 27 to "KEY_RIGHTBRACE",
        28 to "KEY_ENTER",
        29 to "KEY_LEFTCTRL",
        30 to "KEY_A", 31 to "KEY_S", 32 to "KEY_D", 33 to "KEY_F", 34 to "KEY_G",
        35 to "KEY_H", 36 to "KEY_J", 37 to "KEY_K", 38 to "KEY_L",
        39 to "KEY_SEMICOLON", 40 to "KEY_APOSTROPHE", 41 to "KEY_GRAVE",
        42 to "KEY_LEFTSHIFT", 43 to "KEY_BACKSLASH",
        44 to "KEY_Z", 45 to "KEY_X", 46 to "KEY_C", 47 to "KEY_V", 48 to "KEY_B",
        49 to "KEY_N", 50 to "KEY_M",
        51 to "KEY_COMMA", 52 to "KEY_DOT", 53 to "KEY_SLASH",
        54 to "KEY_RIGHTSHIFT", 55 to "KEY_KPASTERISK",
        56 to "KEY_LEFTALT", 57 to "KEY_SPACE", 58 to "KEY_CAPSLOCK",
        59 to "KEY_F1", 60 to "KEY_F2", 61 to "KEY_F3", 62 to "KEY_F4",
        63 to "KEY_F5", 64 to "KEY_F6", 65 to "KEY_F7", 66 to "KEY_F8",
        67 to "KEY_F9", 68 to "KEY_F10",
        69 to "KEY_NUMLOCK", 70 to "KEY_SCROLLLOCK",
        71 to "KEY_KP7", 72 to "KEY_KP8", 73 to "KEY_KP9", 74 to "KEY_KPMINUS",
        75 to "KEY_KP4", 76 to "KEY_KP5", 77 to "KEY_KP6", 78 to "KEY_KPPLUS",
        79 to "KEY_KP1", 80 to "KEY_KP2", 81 to "KEY_KP3", 82 to "KEY_KP0",
        83 to "KEY_KPDOT",
        85 to "KEY_ZENKAKUHANKAKU", 86 to "KEY_102ND",
        87 to "KEY_F11", 88 to "KEY_F12",
        89 to "KEY_RO", 90 to "KEY_KATAKANA", 91 to "KEY_HIRAGANA",
        92 to "KEY_HENKAN", 93 to "KEY_KATAKANAHIRAGANA", 94 to "KEY_MUHENKAN",
        95 to "KEY_KPJPCOMMA", 96 to "KEY_KPENTER", 97 to "KEY_RIGHTCTRL",
        98 to "KEY_KPSLASH", 99 to "KEY_SYSRQ", 100 to "KEY_RIGHTALT",
        101 to "KEY_LINEFEED",
        102 to "KEY_HOME", 103 to "KEY_UP", 104 to "KEY_PAGEUP",
        105 to "KEY_LEFT", 106 to "KEY_RIGHT",
        107 to "KEY_END", 108 to "KEY_DOWN", 109 to "KEY_PAGEDOWN",
        110 to "KEY_INSERT", 111 to "KEY_DELETE",
        112 to "KEY_MACRO", 113 to "KEY_MUTE",
        114 to "KEY_VOLUMEDOWN", 115 to "KEY_VOLUMEUP",
        116 to "KEY_POWER",
        117 to "KEY_KPEQUAL", 118 to "KEY_KPPLUSMINUS", 119 to "KEY_PAUSE",
        120 to "KEY_SCALE", 121 to "KEY_KPCOMMA",
        122 to "KEY_HANGEUL", 123 to "KEY_HANJA", 124 to "KEY_YEN",
        125 to "KEY_LEFTMETA", 126 to "KEY_RIGHTMETA", 127 to "KEY_COMPOSE",
        128 to "KEY_STOP", 129 to "KEY_AGAIN", 130 to "KEY_PROPS", 131 to "KEY_UNDO",
        132 to "KEY_FRONT", 133 to "KEY_COPY", 134 to "KEY_OPEN", 135 to "KEY_PASTE",
        136 to "KEY_FIND", 137 to "KEY_CUT", 138 to "KEY_HELP", 139 to "KEY_MENU",
        140 to "KEY_CALC", 141 to "KEY_SETUP", 142 to "KEY_SLEEP", 143 to "KEY_WAKEUP",
        144 to "KEY_FILE", 145 to "KEY_SENDFILE", 146 to "KEY_DELETEFILE",
        147 to "KEY_XFER", 148 to "KEY_PROG1", 149 to "KEY_PROG2",
        150 to "KEY_WWW", 151 to "KEY_MSDOS", 152 to "KEY_SCREENLOCK",
        153 to "KEY_ROTATE_DISPLAY", 154 to "KEY_CYCLEWINDOWS",
        155 to "KEY_MAIL", 156 to "KEY_BOOKMARKS", 157 to "KEY_COMPUTER",
        158 to "KEY_BACK", 159 to "KEY_FORWARD",
        160 to "KEY_CLOSECD", 161 to "KEY_EJECTCD", 162 to "KEY_EJECTCLOSECD",
        163 to "KEY_NEXTSONG", 164 to "KEY_PLAYPAUSE", 165 to "KEY_PREVIOUSSONG",
        166 to "KEY_STOPCD", 167 to "KEY_RECORD", 168 to "KEY_REWIND",
        169 to "KEY_PHONE", 170 to "KEY_ISO", 171 to "KEY_CONFIG",
        172 to "KEY_HOMEPAGE", 173 to "KEY_REFRESH", 174 to "KEY_EXIT",
        175 to "KEY_MOVE", 176 to "KEY_EDIT",
        177 to "KEY_SCROLLUP", 178 to "KEY_SCROLLDOWN",
        179 to "KEY_KPLEFTPAREN", 180 to "KEY_KPRIGHTPAREN",
        181 to "KEY_NEW", 182 to "KEY_REDO",
        183 to "KEY_F13", 184 to "KEY_F14", 185 to "KEY_F15", 186 to "KEY_F16",
        187 to "KEY_F17", 188 to "KEY_F18", 189 to "KEY_F19", 190 to "KEY_F20",
        191 to "KEY_F21", 192 to "KEY_F22", 193 to "KEY_F23", 194 to "KEY_F24",
        200 to "KEY_PLAYCD", 201 to "KEY_PAUSECD",
        202 to "KEY_PROG3", 203 to "KEY_PROG4",
        204 to "KEY_DASHBOARD", 205 to "KEY_SUSPEND", 206 to "KEY_CLOSE",
        207 to "KEY_PLAY", 208 to "KEY_FASTFORWARD", 209 to "KEY_BASSBOOST",
        210 to "KEY_PRINT", 211 to "KEY_HP", 212 to "KEY_CAMERA",
        213 to "KEY_SOUND", 214 to "KEY_QUESTION", 215 to "KEY_EMAIL",
        216 to "KEY_CHAT", 217 to "KEY_SEARCH", 218 to "KEY_CONNECT",
        219 to "KEY_FINANCE", 220 to "KEY_SPORT", 221 to "KEY_SHOP",
        222 to "KEY_ALTERASE", 223 to "KEY_CANCEL",
        224 to "KEY_BRIGHTNESSDOWN", 225 to "KEY_BRIGHTNESSUP",
        226 to "KEY_MEDIA", 227 to "KEY_SWITCHVIDEOMODE",
        228 to "KEY_KBDILLUMTOGGLE", 229 to "KEY_KBDILLUMDOWN", 230 to "KEY_KBDILLUMUP",
        231 to "KEY_SEND", 232 to "KEY_REPLY", 233 to "KEY_FORWARDMAIL",
        234 to "KEY_SAVE", 235 to "KEY_DOCUMENTS",
        236 to "KEY_BATTERY", 237 to "KEY_BLUETOOTH", 238 to "KEY_WLAN",
        239 to "KEY_UWB", 240 to "KEY_UNKNOWN",
        241 to "KEY_VIDEO_NEXT", 242 to "KEY_VIDEO_PREV",
        243 to "KEY_BRIGHTNESS_CYCLE", 244 to "KEY_BRIGHTNESS_AUTO",
        245 to "KEY_DISPLAY_OFF", 246 to "KEY_WWAN", 247 to "KEY_RFKILL",
        248 to "KEY_MICMUTE",
        // BTN_MISC 0x100–0x10f
        0x100 to "BTN_0", 0x101 to "BTN_1", 0x102 to "BTN_2", 0x103 to "BTN_3",
        0x104 to "BTN_4", 0x105 to "BTN_5", 0x106 to "BTN_6", 0x107 to "BTN_7",
        0x108 to "BTN_8", 0x109 to "BTN_9",
        // BTN_MOUSE 0x110–0x117
        0x110 to "BTN_LEFT", 0x111 to "BTN_RIGHT", 0x112 to "BTN_MIDDLE",
        0x113 to "BTN_SIDE", 0x114 to "BTN_EXTRA",
        0x115 to "BTN_FORWARD", 0x116 to "BTN_BACK", 0x117 to "BTN_TASK",
        // BTN_JOYSTICK 0x120–0x12f
        0x120 to "BTN_TRIGGER", 0x121 to "BTN_THUMB", 0x122 to "BTN_THUMB2",
        0x123 to "BTN_TOP", 0x124 to "BTN_TOP2", 0x125 to "BTN_PINKIE",
        0x126 to "BTN_BASE", 0x127 to "BTN_BASE2", 0x128 to "BTN_BASE3",
        0x129 to "BTN_BASE4", 0x12a to "BTN_BASE5", 0x12b to "BTN_BASE6",
        0x12f to "BTN_DEAD",
        // BTN_GAMEPAD 0x130–0x13e
        0x130 to "BTN_SOUTH", 0x131 to "BTN_EAST", 0x132 to "BTN_C",
        0x133 to "BTN_NORTH", 0x134 to "BTN_WEST", 0x135 to "BTN_Z",
        0x136 to "BTN_TL", 0x137 to "BTN_TR", 0x138 to "BTN_TL2", 0x139 to "BTN_TR2",
        0x13a to "BTN_SELECT", 0x13b to "BTN_START", 0x13c to "BTN_MODE",
        0x13d to "BTN_THUMBL", 0x13e to "BTN_THUMBR",
        // BTN_DIGI 0x140–0x14f
        0x140 to "BTN_TOOL_PEN", 0x141 to "BTN_TOOL_RUBBER",
        0x142 to "BTN_TOOL_BRUSH", 0x143 to "BTN_TOOL_PENCIL",
        0x144 to "BTN_TOOL_AIRBRUSH", 0x145 to "BTN_TOOL_FINGER",
        0x146 to "BTN_TOOL_MOUSE", 0x147 to "BTN_TOOL_LENS",
        0x148 to "BTN_TOOL_QUINTTAP",
        0x149 to "BTN_STYLUS3",
        0x14a to "BTN_TOUCH", 0x14b to "BTN_STYLUS", 0x14c to "BTN_STYLUS2",
        0x14d to "BTN_TOOL_DOUBLETAP", 0x14e to "BTN_TOOL_TRIPLETAP",
        0x14f to "BTN_TOOL_QUADTAP",
        // BTN_WHEEL 0x150–
        0x150 to "BTN_GEAR_DOWN", 0x151 to "BTN_GEAR_UP",
        // Trigger happy 0x2c0–
        0x2c0 to "BTN_TRIGGER_HAPPY1", 0x2c1 to "BTN_TRIGGER_HAPPY2",
        0x2c2 to "BTN_TRIGGER_HAPPY3", 0x2c3 to "BTN_TRIGGER_HAPPY4",
    )

    // ── EV_REL codes ─────────────────────────────────────────────────────────

    private val REL_CODES = mapOf(
        0x00 to "REL_X", 0x01 to "REL_Y", 0x02 to "REL_Z",
        0x03 to "REL_RX", 0x04 to "REL_RY", 0x05 to "REL_RZ",
        0x06 to "REL_HWHEEL", 0x07 to "REL_DIAL", 0x08 to "REL_WHEEL",
        0x09 to "REL_MISC", 0x0b to "REL_WHEEL_HI_RES", 0x0c to "REL_HWHEEL_HI_RES",
    )

    // ── EV_ABS codes ─────────────────────────────────────────────────────────

    private val ABS_CODES = mapOf(
        0x00 to "ABS_X", 0x01 to "ABS_Y", 0x02 to "ABS_Z",
        0x03 to "ABS_RX", 0x04 to "ABS_RY", 0x05 to "ABS_RZ",
        0x06 to "ABS_THROTTLE", 0x07 to "ABS_RUDDER",
        0x08 to "ABS_WHEEL", 0x09 to "ABS_GAS", 0x0a to "ABS_BRAKE",
        0x10 to "ABS_HAT0X", 0x11 to "ABS_HAT0Y",
        0x12 to "ABS_HAT1X", 0x13 to "ABS_HAT1Y",
        0x14 to "ABS_HAT2X", 0x15 to "ABS_HAT2Y",
        0x16 to "ABS_HAT3X", 0x17 to "ABS_HAT3Y",
        0x18 to "ABS_PRESSURE", 0x19 to "ABS_DISTANCE",
        0x1a to "ABS_TILT_X", 0x1b to "ABS_TILT_Y",
        0x1c to "ABS_TOOL_WIDTH",
        0x20 to "ABS_VOLUME", 0x21 to "ABS_PROFILE",
        0x28 to "ABS_MISC",
        0x2e to "ABS_RESERVED",
        0x2f to "ABS_MT_SLOT",
        0x30 to "ABS_MT_TOUCH_MAJOR", 0x31 to "ABS_MT_TOUCH_MINOR",
        0x32 to "ABS_MT_WIDTH_MAJOR", 0x33 to "ABS_MT_WIDTH_MINOR",
        0x34 to "ABS_MT_ORIENTATION",
        0x35 to "ABS_MT_POSITION_X", 0x36 to "ABS_MT_POSITION_Y",
        0x37 to "ABS_MT_TOOL_TYPE",
        0x38 to "ABS_MT_BLOB_ID",
        0x39 to "ABS_MT_TRACKING_ID",
        0x3a to "ABS_MT_PRESSURE",
        0x3b to "ABS_MT_DISTANCE",
        0x3c to "ABS_MT_TOOL_X", 0x3d to "ABS_MT_TOOL_Y",
    )

    // ── EV_MSC codes ─────────────────────────────────────────────────────────

    private val MSC_CODES = mapOf(
        0 to "MSC_SERIAL", 1 to "MSC_PULSELED", 2 to "MSC_GESTURE",
        3 to "MSC_RAW", 4 to "MSC_SCAN", 5 to "MSC_TIMESTAMP",
    )

    // ── EV_SW codes ──────────────────────────────────────────────────────────

    private val SW_CODES = mapOf(
        0x00 to "SW_LID",
        0x01 to "SW_TABLET_MODE",
        0x02 to "SW_HEADPHONE_INSERT",
        0x03 to "SW_RFKILL_ALL",
        0x04 to "SW_MICROPHONE_INSERT",
        0x05 to "SW_DOCK",
        0x06 to "SW_LINEOUT_INSERT",
        0x07 to "SW_JACK_PHYSICAL_INSERT",
        0x08 to "SW_VIDEOOUT_INSERT",
        0x09 to "SW_CAMERA_LENS_COVER",
        0x0a to "SW_KEYPAD_SLIDE",
        0x0b to "SW_FRONT_PROXIMITY",
        0x0c to "SW_ROTATE_LOCK",
        0x0d to "SW_LINEIN_INSERT",
        0x0e to "SW_MUTE_DEVICE",
        0x0f to "SW_PEN_INSERTED",
        0x10 to "SW_MACHINE_COVER",
    )

    // ── EV_LED codes ─────────────────────────────────────────────────────────

    private val LED_CODES = mapOf(
        0 to "LED_NUML", 1 to "LED_CAPSL", 2 to "LED_SCROLLL",
        3 to "LED_COMPOSE", 4 to "LED_KANA", 5 to "LED_SLEEP",
        6 to "LED_SUSPEND", 7 to "LED_MUTE", 8 to "LED_MISC",
        9 to "LED_MAIL", 10 to "LED_CHARGING",
    )

    // ── EV_SND codes ─────────────────────────────────────────────────────────

    private val SND_CODES = mapOf(
        0 to "SND_CLICK", 1 to "SND_BELL", 2 to "SND_TONE",
    )

    // ── EV_REP codes ─────────────────────────────────────────────────────────

    private val REP_CODES = mapOf(
        0 to "REP_DELAY", 1 to "REP_PERIOD",
    )

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns a formatted, human-readable line for one input_event.
     * Example: "EV_KEY        KEY_VOLUMEDOWN                released"
     */
    fun formatEvent(type: Int, code: Int, value: Int): String {
        val typeName = EVENT_TYPES[type] ?: "0x%02x".format(type)
        val codeName = codeNameFor(type, code)
        val valueName = valueNameFor(type, value)
        return "%-14s %-30s %s".format(typeName, codeName, valueName)
    }

    private fun codeNameFor(type: Int, code: Int): String = when (type) {
        0x00 -> SYN_CODES[code]
        0x01 -> KEY_CODES[code]
        0x02 -> REL_CODES[code]
        0x03 -> ABS_CODES[code]
        0x04 -> MSC_CODES[code]
        0x05 -> SW_CODES[code]
        0x11 -> LED_CODES[code]
        0x12 -> SND_CODES[code]
        0x14 -> REP_CODES[code]
        else -> null
    } ?: "0x%03x".format(code)

    private fun valueNameFor(type: Int, value: Int): String = when (type) {
        0x01 -> when (value) { // EV_KEY
            0 -> "released"
            1 -> "pressed"
            2 -> "repeat"
            else -> value.toString()
        }
        0x05 -> when (value) { // EV_SW
            0 -> "off"
            1 -> "on"
            else -> value.toString()
        }
        else -> value.toString()
    }
}
