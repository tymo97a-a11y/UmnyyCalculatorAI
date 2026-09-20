package com.smartcalculator.ai

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var history: TextView
    private lateinit var calculatorDisplay: TextView

    private var currentNumber = "0"
    private var firstNumber: Double? = null
    private var currentOperator: String? = null
    private var shouldStartNewNumber = true

    private val preferencesName = "calculator_history"
    private val historyKey = "history"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.input)
        result = findViewById(R.id.result)
        history = findViewById(R.id.history)
        calculatorDisplay = findViewById(R.id.calculatorDisplay)

        val calculateButton =
            findViewById<Button>(R.id.calculateButton)

        val clearButton =
            findViewById<Button>(R.id.clearButton)

        loadHistory()

        setupCalculatorButtons()

        calculateButton.setOnClickListener {

            val text = input.text.toString().trim()

            if (text.isEmpty()) {
                result.text = "Введите пример или вопрос"
                return@setOnClickListener
            }

            result.text = "🤖 AI считает..."

            sendToAI(text)
        }

        clearButton.setOnClickListener {

            input.text.clear()

            result.text = "Ответ появится здесь"

            clearHistory()
        }
    }

    // =========================
    // ОБЫЧНЫЙ КАЛЬКУЛЯТОР
    // =========================

    private fun setupCalculatorButtons() {

        val numbers = mapOf(
            R.id.button0 to "0",
            R.id.button1 to "1",
            R.id.button2 to "2",
            R.id.button3 to "3",
            R.id.button4 to "4",
            R.id.button5 to "5",
            R.id.button6 to "6",
            R.id.button7 to "7",
            R.id.button8 to "8",
            R.id.button9 to "9"
        )

        for ((id, number) in numbers) {

            findViewById<Button>(id).setOnClickListener {
                addNumber(number)
            }
        }

        findViewById<Button>(R.id.buttonDot)
            .setOnClickListener {
                addDot()
            }

        findViewById<Button>(R.id.buttonPlus)
            .setOnClickListener {
                chooseOperator("+")
            }

        findViewById<Button>(R.id.buttonMinus)
            .setOnClickListener {
                chooseOperator("-")
            }

        findViewById<Button>(R.id.buttonMultiply)
            .setOnClickListener {
                chooseOperator("×")
            }

        findViewById<Button>(R.id.buttonDivide)
            .setOnClickListener {
                chooseOperator("÷")
            }

        findViewById<Button>(R.id.buttonEquals)
            .setOnClickListener {
                calculateResult()
            }

        findViewById<Button>(R.id.buttonClearCalc)
            .setOnClickListener {
                clearCalculator()
            }

        findViewById<Button>(R.id.buttonBackspace)
            .setOnClickListener {
                deleteLastNumber()
            }

        findViewById<Button>(R.id.buttonPercent)
            .setOnClickListener {
                calculatePercent()
            }
    }

    private fun addNumber(number: String) {

        if (shouldStartNewNumber || currentNumber == "0") {
            currentNumber = number
            shouldStartNewNumber = false
        } else {
            currentNumber += number
        }

        updateDisplay()
    }

    private fun addDot() {

        if (shouldStartNewNumber) {
            currentNumber = "0."
            shouldStartNewNumber = false
        } else if (!currentNumber.contains(".")) {
            currentNumber += "."
        }

        updateDisplay()
    }

    private fun chooseOperator(operator: String) {

        val number = currentNumber.toDoubleOrNull() ?: 0.0

        if (firstNumber != null && currentOperator != null && !shouldStartNewNumber) {
            calculateResult()
        }

        firstNumber = number
        currentOperator = operator
        shouldStartNewNumber = true
    }

    private fun calculateResult() {

        val first = firstNumber ?: return
        val second = currentNumber.toDoubleOrNull() ?: return
        val operator = currentOperator ?: return

        val answer: Double

        when (operator) {

            "+" -> {
                answer = first + second
            }

            "-" -> {
                answer = first - second
            }

            "×" -> {
                answer = first * second
            }

            "÷" -> {

                if (second == 0.0) {
                    calculatorDisplay.text = "Ошибка"
                    return
                }

                answer = first / second
            }

            else -> return
        }

        currentNumber = formatNumber(answer)

        calculatorDisplay.text = currentNumber

        firstNumber = null
        currentOperator = null
        shouldStartNewNumber = true
    }

    private fun calculatePercent() {

        val number = currentNumber.toDoubleOrNull() ?: return

        val percent = if (firstNumber != null) {
            firstNumber!! * number / 100.0
        } else {
            number / 100.0
        }

        currentNumber = formatNumber(percent)

        shouldStartNewNumber = true

        updateDisplay()
    }

    private fun clearCalculator() {

        currentNumber = "0"
        firstNumber = null
        currentOperator = null
        shouldStartNewNumber = true

        calculatorDisplay.text = "0"
    }

    private fun deleteLastNumber() {

        if (shouldStartNewNumber) {
            return
        }

        currentNumber =
            if (currentNumber.length <= 1) {
                "0"
            } else {
                currentNumber.dropLast(1)
            }

        if (currentNumber == "-" || currentNumber.isEmpty()) {
            currentNumber = "0"
        }

        updateDisplay()
    }

    private fun updateDisplay() {
        calculatorDisplay.text = currentNumber
    }

    private fun formatNumber(number: Double): String {

        if (number == number.toLong().toDouble()) {
            return number.toLong().toString()
        }

        return String.format(
            Locale.US,
            "%.8f",
            number
        ).trimEnd('0')
            .trimEnd('.')
    }

    // =========================
    // AI
    // =========================

    private fun sendToAI(text: String) {

        val json = JSONObject()
        json.put("text", text)

        val mediaType =
            "application/json; charset=utf-8".toMediaType()

        val body =
            json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(
                "https://umnyy-calculator-ai-server.onrender.com/v1/calculate"
            )
            .post(body)
            .addHeader(
                "Content-Type",
                "application/json"
            )
            .build()

        client.newCall(request).enqueue(
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {
                        result.text =
                            "❌ Ошибка подключения к AI\n\n${e.message}"
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val responseText =
                        response.body?.string()

                    runOnUiThread {

                        if (!response.isSuccessful) {

                            result.text =
                                "❌ Ошибка AI: ${response.code}"

                            return@runOnUiThread
                        }

                        try {

                            val jsonResponse =
                                JSONObject(responseText ?: "{}")

                            val answer =
                                jsonResponse.optString(
                                    "result",
                                    ""
                                )

                            val explanation =
                                jsonResponse.optString(
                                    "explanation",
                                    ""
                                )

                            val finalAnswer =
                                if (explanation.isNotEmpty()) {
                                    "$answer\n\n$explanation"
                                } else {
                                    answer
                                }

                            result.text = finalAnswer

                            addToHistory(
                                text,
                                finalAnswer
                            )

                        } catch (e: Exception) {

                            result.text =
                                "❌ Не удалось обработать ответ AI"
                        }
                    }
                }
            }
        )
    }

    // =========================
    // ИСТОРИЯ
    // =========================

    private fun addToHistory(
        question: String,
        answer: String
    ) {

        val preferences =
            getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        val oldHistory =
            preferences.getString(
                historyKey,
                ""
            ) ?: ""

        val newEntry =
            "Вопрос: $question\nОтвет: $answer"

        val newHistory =
            if (oldHistory.isEmpty()) {
                newEntry
            } else {
                "$newEntry\n\n$oldHistory"
            }

        val limitedHistory =
            newHistory
                .split("\n\n")
                .take(10)
                .joinToString("\n\n")

        preferences.edit()
            .putString(
                historyKey,
                limitedHistory
            )
            .apply()

        history.text = limitedHistory
    }

    private fun loadHistory() {

        val preferences =
            getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        val savedHistory =
            preferences.getString(
                historyKey,
                ""
            ) ?: ""

        history.text =
            if (savedHistory.isEmpty()) {
                "История расчётов пока пуста"
            } else {
                savedHistory
            }
    }

    private fun clearHistory() {

        val preferences =
            getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        preferences.edit()
            .remove(historyKey)
            .apply()

        history.text =
            "История расчётов пока пуста"
    }
}
