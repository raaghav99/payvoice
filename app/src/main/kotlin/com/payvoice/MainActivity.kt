package com.payvoice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var result: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Speaker.init(applicationContext)

        val pad = dp(16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "PayVoice"
            textSize = 24f
            gravity = Gravity.START
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "Enable notification access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "Test voice"
            setOnClickListener { Speaker.speak(applicationContext, "Rahul paid 250 rupees") }
        }, matchWrap())

        root.addView(Switch(this).apply {
            text = "Only selected apps"
            isChecked = AppFilter.isEnabled(applicationContext)
            setOnCheckedChangeListener { _, checked -> AppFilter.setEnabled(applicationContext, checked) }
        }, matchWrap().apply { topMargin = dp(12) })

        root.addView(TextView(this).apply {
            text = "Test parser (paste notification text)"
        }, matchWrap().apply { topMargin = dp(16) })

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            hint = "Rahul paid you Rs 250"
        }
        root.addView(input, matchWrap())

        result = TextView(this).apply {
            textSize = 16f
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(result, matchWrap())

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                showParse(s?.toString().orEmpty())
            }
        })

        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
    }

    private fun showParse(text: String) {
        if (text.isBlank()) {
            result.text = ""
            return
        }
        val info = PaymentParser.parse(text)
        result.text = if (info == null) {
            "Not a payment-received notification (or no amount found)"
        } else {
            "Name: ${info.name ?: "(none)"}\n" +
                "Amount: ${info.amount.toPlainString()}\n" +
                "Speech: ${PaymentParser.speech(info)}"
        }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
