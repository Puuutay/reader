package com.puuuta.reader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvOverlayStatus: TextView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnEnableOverlay: Button
    private lateinit var btnStartBubble: Button
    private lateinit var spinnerLanguage: Spinner
    private lateinit var seekBarSpeed: SeekBar
    private lateinit var tvSpeed: TextView

    val languages = listOf(
        Pair("Automatic (System)", ""),
        Pair("English (US)", "en-US"),
        Pair("English (UK)", "en-GB"),
        Pair("Filipino / Tagalog", "fil-PH"),
        Pair("Danish", "da-DK"),
        Pair("Finnish", "fi-FI"),
        Pair("Norwegian", "nb-NO"),
        Pair("Swedish", "sv-SE"),
        Pair("Turkish", "tr-TR"),
        Pair("German", "de-DE"),
        Pair("French", "fr-FR"),
        Pair("Spanish", "es-ES"),
        Pair("Italian", "it-IT"),
        Pair("Portuguese (BR)", "pt-BR"),
        Pair("Portuguese (PT)", "pt-PT"),
        Pair("Russian", "ru-RU"),
        Pair("Japanese", "ja-JP"),
        Pair("Korean", "ko-KR"),
        Pair("Chinese (Simplified)", "zh-CN"),
        Pair("Chinese (Traditional)", "zh-TW"),
        Pair("Arabic", "ar-SA"),
        Pair("Hindi", "hi-IN"),
        Pair("Dutch", "nl-NL"),
        Pair("Polish", "pl-PL"),
        Pair("Czech", "cs-CZ"),
        Pair("Greek", "el-GR"),
        Pair("Hungarian", "hu-HU"),
        Pair("Romanian", "ro-RO"),
        Pair("Thai", "th-TH"),
        Pair("Vietnamese", "vi-VN"),
        Pair("Indonesian", "id-ID"),
        Pair("Malay", "ms-MY"),
        Pair("Hebrew", "he-IL"),
        Pair("Ukrainian", "uk-UA"),
        Pair("Catalan", "ca-ES"),
        Pair("Slovak", "sk-SK"),
        Pair("Croatian", "hr-HR"),
        Pair("Bulgarian", "bg-BG"),
        Pair("Bengali", "bn-IN"),
        Pair("Tamil", "ta-IN"),
        Pair("Urdu", "ur-PK")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        btnEnableOverlay = findViewById(R.id.btnEnableOverlay)
        btnStartBubble = findViewById(R.id.btnStartBubble)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        seekBarSpeed = findViewById(R.id.seekBarSpeed)
        tvSpeed = findViewById(R.id.tvSpeed)

        val langNames = languages.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, langNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        val prefs = getSharedPreferences("PuuutaReader", MODE_PRIVATE)
        val savedLang = prefs.getString("language", "")
        val savedIndex = languages.indexOfFirst { it.second == savedLang }.takeIf { it >= 0 } ?: 0
        spinnerLanguage.setSelection(savedIndex)

        val savedSpeed = prefs.getFloat("speed", 1.0f)
        val speedProgress = ((savedSpeed - 0.5f) / 0.1f).toInt()
        seekBarSpeed.progress = speedProgress.coerceIn(0, 20)
        tvSpeed.text = "Speech Speed: ${"%.1f".format(savedSpeed)}x"

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putString("language", languages[pos].second).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val speed = 0.5f + (progress * 0.1f)
                tvSpeed.text = "Speech Speed: ${"%.1f".format(speed)}x"
                prefs.edit().putFloat("speed", speed).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnEnableOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        btnStartBubble.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please enable overlay permission first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val intent = Intent(this, FloatingBubbleService::class.java)
            startService(intent)
            Toast.makeText(this, "Bubble started! You can now go to any app.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val accessEnabled = isAccessibilityServiceEnabled()
        val overlayEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        tvAccessibilityStatus.text = if (accessEnabled) "● Accessibility: ON" else "● Accessibility: OFF"
        tvAccessibilityStatus.setTextColor(if (accessEnabled) 0xFF00ff00.toInt() else 0xFFff4444.toInt())

        tvOverlayStatus.text = if (overlayEnabled) "● Overlay: ON" else "● Overlay: OFF"
        tvOverlayStatus.setTextColor(if (overlayEnabled) 0xFF00ff00.toInt() else 0xFFff4444.toInt())
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/${ReaderAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(service)
    }
}
