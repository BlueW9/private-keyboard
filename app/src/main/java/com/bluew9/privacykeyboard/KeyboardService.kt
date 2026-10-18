/*
 * Privacy Keyboard - an offline keyboard with no clipboard, no suggestions, no history.
 * Copyright (C) 2026 BlueW9
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.bluew9.privacykeyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

private val BG = Color.parseColor("#0B0F14")
private val KEY = Color.parseColor("#1C232B")
private val KEY_SPECIAL = Color.parseColor("#141A21")
private val PRESSED = Color.parseColor("#0A4A78")
private val TEXT = Color.parseColor("#E6EDF3")
private val DIM = Color.parseColor("#7F8C99")
private val PRIVACY = Color.parseColor("#5B8DB8")

class KeyboardService : InputMethodService() {

    private enum class Layer { LETTERS, SYM1, SYM2 }

    // Held in memory only for the current session. Nothing is ever saved or sent anywhere.
    private var layer = Layer.LETTERS
    private var arabic = false
    private var shift = 0 // 0 = off, 1 = once, 2 = caps lock
    private var scale = 1f // keyboard size, set from the app (70%..170%)

    private lateinit var root: LinearLayout
    private val handler = Handler(Looper.getMainLooper())

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(3), 0, dp(3), dp(6))
        }
        loadScale()
        render()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val oldScale = scale
        loadScale()
        if (!restarting) {
            val cls = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
            layer = if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE) {
                Layer.SYM1
            } else {
                Layer.LETTERS
            }
            shift = 0
        }
        if (::root.isInitialized && (!restarting || oldScale != scale)) render()
    }

    private fun loadScale() {
        val pct = getSharedPreferences("settings", MODE_PRIVATE).getInt("scale", 100)
        scale = pct.coerceIn(70, 170) / 100f
    }

    // ---------- rendering ----------

    private fun rerender() {
        handler.post { render() }
    }

    private fun render() {
        root.removeAllViews()
        root.addView(privacyBar())
        root.addView(row(plainKeys("1234567890")))
        when (layer) {
            Layer.LETTERS -> if (arabic) arabicRows() else englishRows()
            Layer.SYM1 -> symbolRows(false)
            Layer.SYM2 -> symbolRows(true)
        }
        root.addView(bottomRow())
    }

    private fun privacyBar(): View = TextView(this).apply {
        text = "Privacy"
        gravity = Gravity.CENTER
        setTextColor(PRIVACY)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(24)
        )
    }

    private fun englishRows() {
        root.addView(row(letterKeys("qwertyuiop")))
        root.addView(row(listOf(spacer(0.5f)) + letterKeys("asdfghjkl") + listOf(spacer(0.5f))))
        val shiftLabel = when (shift) {
            0 -> "⇧"
            1 -> "⇑"
            else -> "⇪"
        }
        root.addView(
            row(
                listOf(key(shiftLabel, 1.5f, special = true) {
                    shift = (shift + 1) % 3
                    rerender()
                }) + letterKeys("zxcvbnm") + listOf(backspaceKey())
            )
        )
    }

    private fun arabicRows() {
        root.addView(row(letterKeys("ضصثقفغعهخحجد")))
        root.addView(row(letterKeys("شسيبلاتنمكط")))
        root.addView(row(letterKeys("ذئءؤرىةوزظ") + listOf(backspaceKey())))
    }

    private fun symbolRows(page2: Boolean) {
        val r1 = if (!page2) "@#\$%&-+()/" else "[]{}<>`^|\\"
        val r2 = if (!page2) "*\"':;!?_=~" else "€£¥°•©®™§¶"
        val r3 = if (!page2) "،؟«»…" else "“”‘’¿¡"
        root.addView(row(plainKeys(r1)))
        root.addView(row(plainKeys(r2)))
        val toggleLabel = if (!page2) "=\\<" else "?123"
        val w = if (!page2) 1.4f else 1.2f
        root.addView(
            row(
                listOf(key(toggleLabel, 1.5f, special = true, size = 15f) {
                    layer = if (page2) Layer.SYM1 else Layer.SYM2
                    rerender()
                }) + plainKeys(r3, w) + listOf(backspaceKey())
            )
        )
    }

    private fun bottomRow(): View {
        val sym = layer != Layer.LETTERS
        val comma = if (arabic && !sym) "،" else ","
        val langLabel = if (arabic) "العربية" else "English"
        return row(
            listOf(
                key(if (sym) "ABC" else "?123", 1.5f, special = true, size = 15f) {
                    layer = if (sym) Layer.LETTERS else Layer.SYM1
                    rerender()
                },
                key("🌐", 1f, special = true) {
                    arabic = !arabic
                    layer = Layer.LETTERS
                    shift = 0
                    rerender()
                },
                key(comma, 1f) { typeText(comma) },
                key(langLabel, 4f, size = 14f, textColor = DIM) { typeText(" ") },
                key(".", 1f) { typeText(".") },
                key("⏎", 1.5f, special = true) { enter() }
            )
        )
    }

    // ---------- building blocks ----------

    private fun row(keys: List<View>): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, sdp(50)
        )
        keys.forEach { addView(it) }
    }

    private fun spacer(weight: Float): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, 1, weight)
    }

    private fun letterKeys(s: String): List<View> = s.map { ch ->
        val c = ch.toString()
        val shown = if (!arabic && shift > 0) c.uppercase() else c
        key(shown, 1f, size = if (arabic) 18f else 20f) { typeText(shown) }
    }

    private fun plainKeys(s: String, weight: Float = 1f): List<View> = s.map { ch ->
        val c = ch.toString()
        key(c, weight) { typeText(c) }
    }

    private fun backspaceKey(): View =
        key("⌫", 1.5f, special = true, repeat = true) { backspace() }

    private fun key(
        label: String,
        weight: Float = 1f,
        special: Boolean = false,
        repeat: Boolean = false,
        size: Float = 20f,
        textColor: Int = TEXT,
        onPress: () -> Unit
    ): View {
        val normal = if (special) KEY_SPECIAL else KEY
        val tv = TextView(this)
        tv.text = label
        tv.gravity = Gravity.CENTER
        tv.setTextColor(textColor)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * (0.7f + 0.3f * scale))
        tv.background = rounded(normal)
        tv.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, weight
        ).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }

        val repeater = object : Runnable {
            override fun run() {
                onPress()
                handler.postDelayed(this, 45)
            }
        }
        tv.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.background = rounded(PRESSED)
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onPress()
                    if (repeat) handler.postDelayed(repeater, 400)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(repeater)
                    v.background = rounded(normal)
                }
            }
            true
        }
        return tv
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(6).toFloat()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun sdp(v: Int): Int = (v * scale * resources.displayMetrics.density).toInt()

    // ---------- input actions ----------

    private fun typeText(s: String) {
        currentInputConnection?.commitText(s, 1)
        if (shift == 1) {
            shift = 0
            rerender()
        }
    }

    private fun backspace() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    private fun enter() {
        val ic = currentInputConnection ?: return
        val ei = currentInputEditorInfo
        if (ei == null) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            return
        }
        val action = ei.imeOptions and EditorInfo.IME_MASK_ACTION
        if ((ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0 ||
            action == EditorInfo.IME_ACTION_NONE ||
            action == EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        } else {
            ic.performEditorAction(action)
        }
    }
}
