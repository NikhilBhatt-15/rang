package com.nikhilbhatt.rang

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import java.util.Locale

/**
 * One-time setup screen, meant for whoever installs the app (not for the person using the phone).
 * Each step shows ✅ once done.
 */
class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var preview: ImageView
    private lateinit var steps: LinearLayout
    private lateinit var voiceStatus: TextView
    private var tts: TextToSpeech? = null
    private val isXiaomi = Build.MANUFACTURER.lowercase().let { it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }
        col.addView(text("Rang  •  रंग बैटरी", 26f, bold = true))
        col.addView(text("A colored battery bar for someone who can't read the battery icon. Do the steps once, then hand the phone back.", 15f, Color.DKGRAY))

        preview = ImageView(this).apply { adjustViewBounds = true }
        col.addView(preview, LinearLayout.LayoutParams(dp(120), WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(16); bottomMargin = dp(8)
        })
        col.addView(legend())

        col.addView(header("Setup"))
        steps = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(steps)

        col.addView(header("Bar thickness"))
        col.addView(RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            listOf("Thin" to 8, "Medium" to 12, "Thick" to 18).forEach { (label, v) ->
                addView(RadioButton(this@MainActivity).apply {
                    text = label; textSize = 16f; id = v; isChecked = prefs.thicknessDp == v
                })
            }
            setOnCheckedChangeListener { _, id ->
                prefs.thicknessDp = id
                BarService.start(this@MainActivity, BarService.ACTION_RELAYOUT)
            }
        })

        col.addView(header("Sounds"))
        col.addView(Switch(this).apply {
            text = "Speak in Hindi (\"फ़ोन चार्ज कर दो\")"; textSize = 16f; isChecked = prefs.voice
            setOnCheckedChangeListener { _, on -> prefs.voice = on }
        })
        voiceStatus = text("", 14f, Color.DKGRAY)
        col.addView(voiceStatus)
        col.addView(button("Install Hindi voice") {
            startSafely(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
        })
        col.addView(text("Low alert plays below 15%, 10% and 5%. Full alert plays at 100% while charging. A short chirp plays when the charger goes in.", 14f, Color.DKGRAY))
        val tests = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tests.addView(button("Test low") { BarService.start(this, BarService.ACTION_TEST_LOW) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        tests.addView(button("Test full") { BarService.start(this, BarService.ACTION_TEST_FULL) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        col.addView(tests)

        if (isXiaomi) {
            col.addView(header("Redmi tip"))
            col.addView(text("Open Recent apps, long-press the Rang card and tap the lock 🔒 so MIUI never closes it.", 14f, Color.DKGRAY))
        }

        setContentView(ScrollView(this).apply {
            addView(col)
            // Android 15 draws apps edge-to-edge; keep the text clear of the status bar and our own bar.
            setOnApplyWindowInsetsListener { v, insets ->
                val top = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(android.view.WindowInsets.Type.systemBars()).top
                    else @Suppress("DEPRECATION") insets.systemWindowInsetTop
                val bottom = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom
                    else @Suppress("DEPRECATION") insets.systemWindowInsetBottom
                v.setPadding(0, top + dp(prefs.thicknessDp), 0, bottom)
                insets
            }
        })

        tts = TextToSpeech(this) { st ->
            val ok = st == TextToSpeech.SUCCESS && tts?.isLanguageAvailable(Locale("hi", "IN"))
                ?.let { it >= TextToSpeech.LANG_AVAILABLE } == true
            runOnUiThread {
                voiceStatus.text = if (ok) "✅ Hindi voice is installed." else "⚠️ Hindi voice not found: only beeps will play. Tap Install Hindi voice."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preview.setImageBitmap(BatteryArt.bitmap(Battery.read(this)))
        refreshSteps()
        BarService.start(this)
    }

    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }

    private fun refreshSteps() {
        steps.removeAllViews()
        val overlay = Settings.canDrawOverlays(this)
        steps.addView(step("1. Show the bar over other apps", overlay) {
            startSafely(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        if (isXiaomi) {
            steps.addView(step("1b. MIUI: allow \"Display pop-up windows while running in background\"", null) {
                val miui = Intent("miui.intent.action.APP_PERM_EDITOR")
                    .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                    .putExtra("extra_pkgname", packageName)
                if (!startSafely(miui, quiet = true)) appDetails()
            })
        }
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            steps.addView(step("2. Allow notification (keeps the bar running)", granted) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            })
        }
        val pm = getSystemService(PowerManager::class.java)
        steps.addView(step("3. Battery: no restrictions", pm.isIgnoringBatteryOptimizations(packageName)) {
            if (!startSafely(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")), quiet = true)) appDetails()
        })
        if (isXiaomi) {
            steps.addView(step("4. MIUI: turn on Autostart", null) {
                val auto = Intent().setComponent(ComponentName(
                    "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
                if (!startSafely(auto, quiet = true)) appDetails()
            })
        }
        steps.addView(step("5. Add the big battery widget to the home screen (long-press home → Widgets → Rang)", null) {})
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        refreshSteps(); BarService.start(this)
    }

    private fun appDetails() = startSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    private fun startSafely(i: Intent, quiet: Boolean = false): Boolean = try {
        startActivity(i); true
    } catch (e: Exception) { if (!quiet) android.widget.Toast.makeText(this, "Couldn't open that screen", android.widget.Toast.LENGTH_SHORT).show(); false }

    // --- tiny view helpers ---

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float, color: Int = Color.BLACK, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun header(s: String) = text(s, 19f, bold = true).apply { setPadding(0, dp(22), 0, dp(6)) }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { onClick() }
    }

    /** done = true/false shows ✅/❌; null means the app can't check it. */
    private fun step(label: String, done: Boolean?, onClick: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(6), 0, dp(6))
        addView(text(when (done) { true -> "✅"; false -> "❌"; null -> "▫️" }, 20f), LinearLayout.LayoutParams(dp(36), WRAP_CONTENT))
        addView(text(label, 15f), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        if (done != true && label.startsWith("5.").not()) {
            addView(button("Open", onClick))
        }
    }

    private fun legend(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        listOf(
            Zone.GREEN to "Green: 60–100%",
            Zone.YELLOW to "Yellow: 35–59%",
            Zone.ORANGE to "Orange: 15–34%",
            Zone.RED to "Red (blinks): below 15% — charge now",
        ).forEach { (z, label) ->
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(View(this@MainActivity).apply {
                    background = GradientDrawable().apply { setColor(z.color); cornerRadius = dp(4).toFloat() }
                }, LinearLayout.LayoutParams(dp(40), dp(14)).apply { rightMargin = dp(10) })
                addView(text(label, 15f), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            })
        }
        addView(text("While charging the bar gently pulses and the widget shows ⚡.", 14f, Color.DKGRAY))
    }
}
