package com.nikhilbhatt.rang

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** The four color zones. Colors are ARGB. */
enum class Zone(val color: Int) {
    GREEN(0xFF22C55E.toInt()),
    YELLOW(0xFFFACC15.toInt()),
    ORANGE(0xFFFF7A00.toInt()),
    RED(0xFFE5132B.toInt()),
}

data class BatteryState(val level: Int, val charging: Boolean) {
    val zone: Zone get() = Battery.zone(level)
}

object Battery {
    /** Low-battery voice alerts fire when the level drops below each of these (once per discharge). */
    val LOW_ALERTS = intArrayOf(15, 10, 5)

    fun zone(level: Int): Zone = when {
        level >= 60 -> Zone.GREEN
        level >= 35 -> Zone.YELLOW
        level >= 15 -> Zone.ORANGE
        else -> Zone.RED
    }

    /**
     * Low thresholds the level has newly dropped below, given the ones already alerted since the
     * phone was last charging. A jump past several at once (phone asleep) yields them all, so the
     * caller alerts once and marks them all.
     */
    fun newlyCrossed(level: Int, charging: Boolean, alerted: Set<Int>): Set<Int> =
        if (charging) emptySet()
        else LOW_ALERTS.filter { level < it && it !in alerted }.toSet()

    fun fromIntent(i: Intent): BatteryState {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val pct = if (level < 0) 100 else (level * 100 / scale).coerceIn(0, 100)
        return BatteryState(pct, plugged != 0)
    }

    /** Current state from the sticky battery broadcast. */
    fun read(ctx: Context): BatteryState {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return if (i != null) fromIntent(i) else BatteryState(100, false)
    }
}
