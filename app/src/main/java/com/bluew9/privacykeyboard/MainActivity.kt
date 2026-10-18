/*
 * Privacy Keyboard
 * Copyright (C) 2026 BlueW9
 * Licensed under the GNU General Public License v3.0 or later.
 * See <https://www.gnu.org/licenses/gpl-3.0.html>.
 */
package com.bluew9.privacykeyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            setBackgroundColor(Color.parseColor("#0B0F14"))
        }

        fun text(t: String, sp: Float, color: String) = TextView(this).apply {
            text = t
            setTextColor(Color.parseColor(color))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            setPadding(0, 0, 0, pad / 2)
        }

        col.addView(text("Privacy Keyboard", 26f, "#E6EDF3"))
        col.addView(
            text(
                "Offline. No clipboard. No suggestions. No history.\n" +
                    "بلا إنترنت، بلا حافظة، بلا اقتراحات، بلا سجل.",
                15f, "#7F8C99"
            )
        )
        col.addView(Button(this).apply {
            text = "1) Enable keyboard / تفعيل لوحة المفاتيح"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        })
        col.addView(Button(this).apply {
            text = "2) Select keyboard / اختيار لوحة المفاتيح"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        })
        col.addView(EditText(this).apply {
            hint = "Test here / جرّب هنا"
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#7F8C99"))
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }
}
