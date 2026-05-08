package com.puuuta.reader

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import java.util.Locale

class ReaderAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lastSpokenText = ""

    companion object {
        var instance: ReaderAccessibilityService? = null

        fun speak(text: String) {
            instance?.speakText(text)
        }

        fun stopSpeaking() {
            instance?.tts?.stop()
        }

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            applySettings()
        }
    }

    private fun applySettings() {
        val prefs = getSharedPreferences("PuuutaReader", MODE_PRIVATE)
        val langCode = prefs.getString("language", "") ?: ""
        val speed = prefs.getFloat("speed", 1.0f)

        val locale = if (langCode.isNotEmpty()) {
            Locale.forLanguageTag(langCode)
        } else {
            Locale.getDefault()
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.ENGLISH
        }

        tts?.setSpeechRate(speed)
    }

    fun speakText(text: String) {
        if (!isTtsReady) return
        applySettings()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "puuuta_tts")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isTtsReady || event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            val node = event.source ?: return
            val text = node.text?.toString() ?: return
            val start = node.textSelectionStart
            val end = node.textSelectionEnd

            if (start >= 0 && end > start && end <= text.length) {
                val selected = text.substring(start, end).trim()
                if (selected.isNotEmpty() && selected != lastSpokenText) {
                    lastSpokenText = selected
                    speakText(selected)

                    val intent = Intent("com.puuuta.reader.SELECTED_TEXT")
                    intent.putExtra("text", selected)
                    sendBroadcast(intent)
                }
            }
        }
    }

    override fun onInterrupt() {
        tts?.stop()
    }

    override fun onDestroy() {
        instance = null
        tts?.shutdown()
        super.onDestroy()
    }
}
