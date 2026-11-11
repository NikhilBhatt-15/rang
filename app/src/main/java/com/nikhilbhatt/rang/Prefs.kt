package com.nikhilbhatt.rang

import android.content.Context

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("rang", Context.MODE_PRIVATE)

    /** Bar thickness in dp. */
    var thicknessDp: Int
        get() = p.getInt("thickness", 12)
        set(v) = p.edit().putInt("thickness", v).apply()

    var voice: Boolean
        get() = p.getBoolean("voice", true)
        set(v) = p.edit().putBoolean("voice", v).apply()

    var lowAlerted: Set<Int>
        get() = p.getStringSet("lowAlerted", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
        set(v) = p.edit().putStringSet("lowAlerted", v.map { it.toString() }.toSet()).apply()

    var fullAlerted: Boolean
        get() = p.getBoolean("fullAlerted", false)
        set(v) = p.edit().putBoolean("fullAlerted", v).apply()
}
