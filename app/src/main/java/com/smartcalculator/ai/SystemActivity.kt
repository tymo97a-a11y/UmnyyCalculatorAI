package com.smartcalculator.ai

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SystemActivity : AppCompatActivity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(30))
            setBackgroundColor(Color.rgb(7, 11, 20))
        }
        setContentView(ScrollView(this).apply { addView(root) })

        val back = Button(this).apply {
            text = "← Назад"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 16f
            setOnClickListener { finish() }
        }
        root.addView(back, params(52))
        root.addView(title("🧩 Системы уравнений"))
        root.addView(label("Решение двух линейных уравнений без AI и API."))
        root.addView(label("Первое уравнение"))
        val first = input("Например: 2x + y = 7")
        root.addView(first, params(64))
        root.addView(label("Второе уравнение"))
        val second = input("Например: x - y = 1")
        root.addView(second, params(64))
        val solve = Button(this).apply {
            text = "🧮 Решить без AI"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(36, 107, 253))
            textSize = 16f
        }
        root.addView(solve, params(58))
        val result = TextView(this).apply {
            text = "Введите два уравнения вида a₁x + b₁y = c₁."
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.rgb(17, 26, 43))
        }
        root.addView(result, params())

        solve.setOnClickListener {
            result.text = try {
                SystemSolver.solve(first.text.toString(), second.text.toString()).let {
                    "${it.result}\n\n${it.explanation}"
                }
            } catch (exception: Exception) {
                "Ошибка ввода: ${exception.message ?: "введите два линейных уравнения."}"
            }
        }
    }

    private fun params(height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (height == ViewGroup.LayoutParams.WRAP_CONTENT) height else dp(height)
        ).apply { topMargin = dp(12) }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 27f
        setPadding(0, dp(8), 0, dp(4))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.rgb(180, 195, 215))
        textSize = 14f
    }

    private fun input(hint: String) = EditText(this).apply {
        this.hint = hint
        setTextColor(Color.WHITE)
        setHintTextColor(Color.rgb(145, 160, 184))
        textSize = 16f
        isSingleLine = true
        setPadding(dp(16), 0, dp(16), 0)
        setBackgroundColor(Color.rgb(17, 26, 43))
    }
}
