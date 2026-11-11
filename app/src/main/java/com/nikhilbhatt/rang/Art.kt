package com.nikhilbhatt.rang

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** The always-on bar: a dark "tube" the full width of the screen, filled with color up to the battery level. */
class BarView(ctx: Context) : View(ctx) {
    private val track = Paint().apply { color = 0xFF2A2A2A.toInt() }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    var state = BatteryState(100, false)
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        c.drawRect(0f, 0f, w, h, track)
        fill.color = state.zone.color
        // Keep at least a sliver visible at 0-2% so the red never disappears.
        val fw = (w * state.level / 100f).coerceAtLeast(h * 2)
        c.drawRect(0f, 0f, fw, h, fill)
    }
}

/** Draws a big upright battery with no text: color fill by level, lightning bolt while charging. */
object BatteryArt {
    fun bitmap(s: BatteryState, w: Int = 300, h: Int = 450): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val stroke = w * 0.07f
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = stroke; color = 0xFF222222.toInt()
        }
        val solid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF222222.toInt() }
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF2F2F2.toInt() }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.zone.color }

        val tipH = h * 0.07f
        c.drawRoundRect(RectF(w * 0.33f, 0f, w * 0.67f, tipH + stroke), stroke, stroke, solid)
        val body = RectF(stroke / 2, tipH + stroke / 2, w - stroke / 2, h - stroke / 2)
        val r = w * 0.12f
        c.drawRoundRect(body, r, r, bg)

        val inset = stroke * 1.4f
        val inner = RectF(body.left + inset, body.top + inset, body.right - inset, body.bottom - inset)
        val frac = (s.level / 100f).coerceAtLeast(0.04f)
        val top = inner.bottom - inner.height() * frac
        c.drawRoundRect(RectF(inner.left, top, inner.right, inner.bottom), r * 0.6f, r * 0.6f, fill)
        c.drawRoundRect(body, r, r, outline)

        if (s.charging) {
            val cx = w / 2f; val cy = body.centerY(); val u = w * 0.22f
            val bolt = Path().apply {
                moveTo(cx + u * 0.3f, cy - u * 1.6f)
                lineTo(cx - u * 0.9f, cy + u * 0.2f)
                lineTo(cx - u * 0.05f, cy + u * 0.2f)
                lineTo(cx - u * 0.35f, cy + u * 1.6f)
                lineTo(cx + u * 0.9f, cy - u * 0.25f)
                lineTo(cx + u * 0.05f, cy - u * 0.25f)
                close()
            }
            c.drawPath(bolt, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
            c.drawPath(bolt, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = stroke * 0.5f; color = 0xFF222222.toInt()
                strokeJoin = Paint.Join.ROUND
            })
        }
        return bmp
    }
}
