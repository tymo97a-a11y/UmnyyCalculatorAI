package com.smartcalculator.ai

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.input)
        val calculateButton = findViewById<Button>(R.id.calculateButton)
        val result = findViewById<TextView>(R.id.result)

        calculateButton.setOnClickListener {
            val text = input.text.toString().trim()

            if (text.isEmpty()) {
                result.text = "Введите пример"
            } else {
                try {
                    val answer = calculate(text)
                    result.text = "Ответ: $answer"
                } catch (e: Exception) {
                    result.text = "Не удалось решить пример"
                }
            }
        }
    }

    private fun calculate(text: String): Double {
        val clean = text.replace(" ", "")

        return when {
            clean.contains("+") -> {
                val parts = clean.split("+")
                parts[0].toDouble() + parts[1].toDouble()
            }

            clean.contains("-") -> {
                val parts = clean.split("-")
                parts[0].toDouble() - parts[1].toDouble()
            }

            clean.contains("*") -> {
                val parts = clean.split("*")
                parts[0].toDouble() * parts[1].toDouble()
            }

            clean.contains("/") -> {
                val parts = clean.split("/")
                parts[0].toDouble() / parts[1].toDouble()
            }

            else -> clean.toDouble()
        }
    }
}
