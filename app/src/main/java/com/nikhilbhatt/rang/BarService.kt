package com.nikhilbhatt.rang

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.app.ServiceCompat

/**
 * Keeps the colored bar on screen over every app, refreshes the home-screen widget,
 * and plays the low / full / plugged-in sounds.
 */
class BarService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private lateinit var alerts: Alerts
    private var bar: BarView? = null
    private var anim: ObjectAnimator? = null
    private var last: BatteryState? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = onBattery(Battery.fromIntent(i))
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Prefs(this)
        alerts = Alerts(this)
        ServiceCompat.startForeground(
            this, NOTIF_ID, notification(Battery.read(this)),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        addBar()
        // The battery broadcast is sticky, so registering delivers the current state immediately.
        registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RELAYOUT -> { removeBar(); addBar(); last?.let { applyToBar(it) } }
            ACTION_TEST_LOW -> alerts.low(prefs.voice)
            ACTION_TEST_FULL -> alerts.full(prefs.voice)
        }
        return START_STICKY
    }

    private fun addBar() {
        if (!Settings.canDrawOverlays(this) || bar != null) return
        val px = (prefs.thicknessDp * resources.displayMetrics.density).toInt()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, px,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
            // Android 12+ only lets touches pass through overlays at <= 80% opacity.
            alpha = 0.8f
            // Sit just below the status bar (at the very top when an app hides it).
            if (Build.VERSION.SDK_INT >= 30) fitInsetsTypes = WindowInsets.Type.statusBars()
        }
        val v = BarView(this)
        try { wm.addView(v, lp); bar = v } catch (e: RuntimeException) { /* permission revoked */ }
    }

    private fun removeBar() {
        anim?.cancel(); anim = null
        bar?.let { runCatching { wm.removeView(it) } }
        bar = null
    }

    private fun onBattery(s: BatteryState) {
        val prev = last
        if (prev == s) return  // the broadcast also fires for voltage/temperature changes
        last = s
        applyToBar(s)
        BatteryWidget.refresh(this, s)
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notification(s))

        if (s.charging) {
            prefs.lowAlerted = emptySet()
            if (prev != null && !prev.charging) alerts.pluggedIn()
            if (s.level >= 100 && !prefs.fullAlerted) { prefs.fullAlerted = true; alerts.full(prefs.voice) }
        } else {
            prefs.fullAlerted = false
            val crossed = Battery.newlyCrossed(s.level, false, prefs.lowAlerted)
            if (crossed.isNotEmpty()) {
                prefs.lowAlerted = prefs.lowAlerted + crossed
                alerts.low(prefs.voice)
            }
        }
    }

    private fun applyToBar(s: BatteryState) {
        val v = bar ?: return
        v.state = s
        anim?.cancel(); v.alpha = 1f
        anim = when {
            // Red and not charging: fast blink.
            s.zone == Zone.RED && !s.charging -> pulse(v, 0.15f, 450)
            // Charging: slow breathing.
            s.charging && s.level < 100 -> pulse(v, 0.45f, 1100)
            else -> null
        }
    }

    private fun pulse(v: View, to: Float, ms: Long) =
        ObjectAnimator.ofFloat(v, View.ALPHA, 1f, to).apply {
            duration = ms
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

    private fun notification(s: BatteryState): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_MIN)
                    .apply { setShowBadge(false) }
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setContentTitle("${s.level}%" + if (s.charging) " ⚡" else "")
            .setColor(s.zone.color)
            .setColorized(false)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(receiver) }
        removeBar()
        alerts.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "bar"
        private const val NOTIF_ID = 1
        const val ACTION_RELAYOUT = "relayout"
        const val ACTION_TEST_LOW = "test_low"
        const val ACTION_TEST_FULL = "test_full"

        fun start(ctx: Context, action: String? = null) {
            if (!Settings.canDrawOverlays(ctx)) return
            val i = Intent(ctx, BarService::class.java).setAction(action)
            runCatching { ctx.startForegroundService(i) }
        }
    }
}
