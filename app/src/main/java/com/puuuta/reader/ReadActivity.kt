package com.puuuta.reader

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ReadActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var textToRead = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToRead = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        if (textToRead.isEmpty()) {
            Toast.makeText(this, "No text to read!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
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
            tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "puuuta_read")

            Toast.makeText(this, "Reading...", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
