package com.puuuta.reader

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var menuView: View
    private var isMenuVisible = false
    private var lastSelectedText = ""

    private val textReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            lastSelectedText = intent?.getStringExtra("text") ?: ""
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        registerReceiver(textReceiver, IntentFilter("com.puuuta.reader.SELECTED_TEXT"))

        createBubble()
        createMenu()
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun createBubble() {
        val inflater = LayoutInflater.from(this)
        bubbleView = inflater.inflate(R.layout.bubble_layout, null)

        val params = WindowManager.LayoutParams(
            140, 140,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 16
        params.y = 200

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubbleView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isDragging = true
                    if (isDragging) {
                        params.x = initialX - dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(bubbleView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) toggleMenu()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubbleView, params)
    }

    private fun createMenu() {
        val inflater = LayoutInflater.from(this)
        menuView = inflater.inflate(R.layout.menu_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 16
        params.y = 360

        menuView.visibility = View.GONE

        menuView.findViewById<Button>(R.id.btnRead).setOnClickListener {
            if (lastSelectedText.isNotEmpty()) {
                ReaderAccessibilityService.speak(lastSelectedText)
            } else {
                Toast.makeText(this, "Select text first!", Toast.LENGTH_SHORT).show()
            }
            hideMenu()
        }

        menuView.findViewById<Button>(R.id.btnStop).setOnClickListener {
            ReaderAccessibilityService.stopSpeaking()
            hideMenu()
        }

        menuView.findViewById<Button>(R.id.btnVoice).setOnClickListener {
            hideMenu()
            showVoicePicker()
        }

        menuView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            stopSelf()
        }

        windowManager.addView(menuView, params)
    }

    private fun showVoicePicker() {
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
            Pair("Russian", "ru-RU"),
            Pair("Japanese", "ja-JP"),
            Pair("Korean", "ko-KR"),
            Pair("Chinese (Simplified)", "zh-CN"),
            Pair("Arabic", "ar-SA"),
            Pair("Hindi", "hi-IN"),
            Pair("Dutch", "nl-NL"),
            Pair("Polish", "pl-PL"),
            Pair("Indonesian", "id-ID"),
            Pair("Malay", "ms-MY"),
            Pair("Vietnamese", "vi-VN"),
            Pair("Thai", "th-TH"),
            Pair("Ukrainian", "uk-UA")
        )

        val prefs = getSharedPreferences("PuuutaReader", MODE_PRIVATE)
        val currentLang = prefs.getString("language", "") ?: ""
        val currentIndex = languages.indexOfFirst { it.second == currentLang }.takeIf { it >= 0 } ?: 0

        val langNames = languages.map { it.first }.toTypedArray()

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 16)
        }

        val listView = ListView(this).apply {
            adapter = ArrayAdapter(this@FloatingBubbleService, android.R.layout.simple_list_item_single_choice, langNames)
            choiceMode = ListView.CHOICE_MODE_SINGLE
            setItemChecked(currentIndex, true)
        }
        dialogView.addView(listView, LinearLayout.LayoutParams(-1, 600))

        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )
        dialogParams.dimAmount = 0.7f

        val dialogContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1a1a1a.toInt())
            setPadding(0, 0, 0, 0)
        }

        val title = TextView(this).apply {
            text = "Select Language / Voice"
            setTextColor(0xFF00ff00.toInt())
            textSize = 16f
            setPadding(32, 24, 32, 16)
        }

        val btnContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(16, 8, 16, 16)
        }

        val btnCancel = Button(this).apply {
            text = "Cancel"
            setTextColor(0xFF888888.toInt())
            setBackgroundColor(0x00000000)
        }

        val btnOk = Button(this).apply {
            text = "Apply"
            setTextColor(0xFF00ff00.toInt())
            setBackgroundColor(0x00000000)
        }

        btnContainer.addView(btnCancel)
        btnContainer.addView(btnOk)

        dialogContainer.addView(title)
        dialogContainer.addView(listView, LinearLayout.LayoutParams(-1, 800))
        dialogContainer.addView(btnContainer)

        windowManager.addView(dialogContainer, dialogParams)

        btnCancel.setOnClickListener {
            windowManager.removeView(dialogContainer)
        }

        btnOk.setOnClickListener {
            val selected = listView.checkedItemPosition
            if (selected >= 0) {
                prefs.edit().putString("language", languages[selected].second).apply()
                Toast.makeText(this, "Language: ${languages[selected].first}", Toast.LENGTH_SHORT).show()
            }
            windowManager.removeView(dialogContainer)
        }
    }

    private fun toggleMenu() {
        if (isMenuVisible) hideMenu() else showMenu()
    }

    private fun showMenu() {
        menuView.visibility = View.VISIBLE
        isMenuVisible = true
    }

    private fun hideMenu() {
        menuView.visibility = View.GONE
        isMenuVisible = false
    }

    override fun onDestroy() {
        unregisterReceiver(textReceiver)
        if (::bubbleView.isInitialized) windowManager.removeView(bubbleView)
        if (::menuView.isInitialized) windowManager.removeView(menuView)
        super.onDestroy()
    }
}
