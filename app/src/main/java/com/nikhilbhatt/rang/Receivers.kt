package com.nikhilbhatt.rang

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Brings the bar back after a restart or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = BarService.start(ctx)
}

/** Home-screen widget: one big battery picture, no numbers. */
class BatteryWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        refresh(ctx, Battery.read(ctx))
        BarService.start(ctx)  // the service keeps the widget current from here on
    }

    companion object {
        fun refresh(ctx: Context, s: BatteryState) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val cn = ComponentName(ctx, BatteryWidget::class.java)
            if (mgr.getAppWidgetIds(cn).isEmpty()) return
            val rv = RemoteViews(ctx.packageName, R.layout.widget).apply {
                setImageViewBitmap(R.id.battery, BatteryArt.bitmap(s))
            }
            mgr.updateAppWidget(cn, rv)
        }
    }
}
