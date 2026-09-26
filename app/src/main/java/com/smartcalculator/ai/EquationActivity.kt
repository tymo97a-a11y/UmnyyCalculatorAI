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

class EquationActivity : AppCompatActivity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(30))
            setBackgroundColor(Color.rgb(7, 11, 20))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(title("🧠 Уравнения и функции", 27f))
        root.addView(label("Локальный математический модуль — работает без интернета."))
        val equation = input("Например: x² - 5x + 6 = 0")
        root.addView(equation, params(76))
        val solve = button("🧮 Решить")
        root.addView(solve, params(58))
        val answer = output("Введите линейное или квадратное уравнение.")
        root.addView(answer, params())

        root.addView(title("📈 График функции", 22f))
        root.addView(label("Введите выражение без знака =. Поддерживаются x, +, −, ×, ÷, ^, скобки, sin, cos, tan, sqrt, abs, ln, log, π и e."))
        val function = input("Например: x^2 - 4 или 2*x + 5")
        root.addView(function, params(76))
        val graphButton = button("📈 Построить график")
        root.addView(graphButton, params(58))
        val graph = GraphView(this)
        root.addView(graph, params(420))

        solve.setOnClickListener {
            answer.text = try {
                EquationSolver.solve(equation.text.toString()).let { "${it.result}\n\n${it.explanation}" }
            } catch (exception: Exception) { "Ошибка: ${exception.message ?: "не удалось разобрать уравнение"}" }
        }
        graphButton.setOnClickListener {
            val expression = function.text.toString().trim()
            if (expression.isBlank() || expression.contains('=')) {
                answer.text = "Для графика функции введи выражение, например: 2*x+5"
                return@setOnClickListener
            }

            try {
                graph.setFunction(EquationSolver.compileFunction(expression))
                answer.text = "График построен локально."
            } catch (exception: Exception) { answer.text = "Ошибка функции: ${exception.message ?: "не удалось разобрать функцию"}" }
        }
    }

    private fun params(height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, if (height == ViewGroup.LayoutParams.WRAP_CONTENT) height else dp(height)).apply { topMargin = dp(12) }
    private fun title(text: String, size: Float) = TextView(this).apply { this.text = text; setTextColor(Color.WHITE); textSize = size; setPadding(0, dp(8), 0, dp(4)) }
    private fun label(text: String) = TextView(this).apply { this.text = text; setTextColor(Color.rgb(180, 195, 215)); textSize = 14f; setPadding(0, 0, 0, dp(4)) }
    private fun input(hint: String) = EditText(this).apply { this.hint = hint; setTextColor(Color.WHITE); setHintTextColor(Color.rgb(145, 160, 184)); textSize = 16f; setPadding(dp(16), 0, dp(16), 0); setBackgroundColor(Color.rgb(17, 26, 43)); isSingleLine = true }
    private fun output(text: String) = TextView(this).apply { this.text = text; setTextColor(Color.WHITE); textSize = 16f; setPadding(dp(16), dp(16), dp(16), dp(16)); setBackgroundColor(Color.rgb(17, 26, 43)) }
    private fun button(text: String) = Button(this).apply { this.text = text; setTextColor(Color.WHITE); textSize = 16f; setBackgroundColor(Color.rgb(36, 107, 253)) }
}
