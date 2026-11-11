package com.nikhilbhatt.rang

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Beeps plus a spoken Hindi line. If no Hindi voice is installed, the beeps still play. */
class Alerts(ctx: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    var hindiReady = false
        private set

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("hi", "IN"))
                hindiReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setSpeechRate(0.85f)
            }
        }
    }

    /** Low battery: three urgent beeps on the alarm stream (plays even on silent), then speech. */
    fun low(withVoice: Boolean) = play(
        AudioManager.STREAM_ALARM, AudioAttributes.USAGE_ALARM,
        ToneGenerator.TONE_PROP_BEEP2, repeats = 3,
        speech = if (withVoice) "फ़ोन की बैटरी कम है। फ़ोन चार्ज कर दो।" else null,
    )

    fun full(withVoice: Boolean) = play(
        AudioManager.STREAM_NOTIFICATION, AudioAttributes.USAGE_NOTIFICATION,
        ToneGenerator.TONE_PROP_ACK, repeats = 2,
        speech = if (withVoice) "फ़ोन पूरा चार्ज हो गया।" else null,
    )

    /** Short happy chirp when the charger goes in, so she knows it is charging. */
    fun pluggedIn() = play(
        AudioManager.STREAM_NOTIFICATION, AudioAttributes.USAGE_NOTIFICATION,
        ToneGenerator.TONE_PROP_ACK, repeats = 1, speech = null,
    )

    private fun play(stream: Int, usage: Int, tone: Int, repeats: Int, speech: String?) {
        val gen = try { ToneGenerator(stream, 100) } catch (e: RuntimeException) { null }
        val gap = 450L
        for (i in 0 until repeats) main.postDelayed({ gen?.startTone(tone, 250) }, i * gap)
        main.postDelayed({
            gen?.release()
            if (speech != null && hindiReady) {
                tts?.setAudioAttributes(AudioAttributes.Builder().setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "rang")
            }
        }, repeats * gap + 150)
    }

    fun shutdown() { tts?.shutdown(); tts = null }
}
