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
import java.util.Stack

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var history: TextView
    private lateinit var calculatorDisplay: TextView

    // Полное выражение калькулятора
    private var expression = ""

    // После получения результата начинаем новое число
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

        // ==========================
        // AI
        // ==========================

        calculateButton.setOnClickListener {

            val text =
                input.text.toString().trim()

            if (text.isEmpty()) {

                result.text =
                    "Введите пример или вопрос"

                return@setOnClickListener
            }

            result.text =
                "🤖 AI считает..."

            sendToAI(text)
        }

        clearButton.setOnClickListener {

            input.text.clear()

            result.text =
                "Ответ появится здесь"

            clearHistory()
        }
    }

    // =====================================================
    // КНОПКИ КАЛЬКУЛЯТОРА
    // =====================================================

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

            findViewById<Button>(id)
                .setOnClickListener {

                    addNumber(number)
                }
        }

        findViewById<Button>(R.id.buttonDot)
            .setOnClickListener {

                addDot()
            }

        findViewById<Button>(R.id.buttonPlus)
            .setOnClickListener {

                addOperator("+")
            }

        findViewById<Button>(R.id.buttonMinus)
            .setOnClickListener {

                addOperator("-")
            }

        findViewById<Button>(R.id.buttonMultiply)
            .setOnClickListener {

                addOperator("×")
            }

        findViewById<Button>(R.id.buttonDivide)
            .setOnClickListener {

                addOperator("÷")
            }

        findViewById<Button>(R.id.buttonEquals)
            .setOnClickListener {

                calculateExpression()
            }

        findViewById<Button>(R.id.buttonClearCalc)
            .setOnClickListener {

                clearCalculator()
            }

        findViewById<Button>(R.id.buttonBackspace)
            .setOnClickListener {

                deleteLast()
            }

        findViewById<Button>(R.id.buttonPercent)
            .setOnClickListener {

                addPercent()
            }
    }

    // =====================================================
    // ЧИСЛА
    // =====================================================

    private fun addNumber(number: String) {

        // Если был показан результат,
        // начинаем новое выражение
        if (shouldStartNewNumber) {

            expression = number

            shouldStartNewNumber = false

        } else {

            // Не даём получить 00025
            if (expression == "0") {

                expression = number

            } else {

                expression += number
            }
        }

        updateDisplay()
    }

    // =====================================================
    // ТОЧКА
    // =====================================================

    private fun addDot() {

        if (shouldStartNewNumber) {

            expression = "0."

            shouldStartNewNumber = false

            updateDisplay()

            return
        }

        // Находим последнее число
        val lastNumber =
            getLastNumber(expression)

        // Если в текущем числе уже есть точка —
        // вторую не добавляем
        if (lastNumber.contains(".")) {

            return
        }

        // Если выражение заканчивается оператором
        if (
            expression.endsWith("+") ||
            expression.endsWith("-") ||
            expression.endsWith("×") ||
            expression.endsWith("÷")
        ) {

            expression += "0."

        } else {

            expression += "."
        }

        updateDisplay()
    }

    // =====================================================
    // ОПЕРАТОР
    // =====================================================

    private fun addOperator(operator: String) {

        if (expression.isEmpty()) {

            return
        }

        // Нельзя поставить два оператора подряд
        if (
            expression.endsWith("+") ||
            expression.endsWith("-") ||
            expression.endsWith("×") ||
            expression.endsWith("÷")
        ) {

            expression =
                expression.dropLast(1) + operator

        } else {

            expression += operator
        }

        shouldStartNewNumber = false

        updateDisplay()
    }

    // =====================================================
    // ПРОЦЕНТ
    // =====================================================

    private fun addPercent() {

        if (expression.isEmpty()) {

            return
        }

        val lastNumber =
            getLastNumber(expression)

        val number =
            lastNumber.toDoubleOrNull()

        if (number == null) {

            return
        }

        val percent =
            number / 100.0

        val formatted =
            formatNumber(percent)

        expression =
            expression.dropLast(lastNumber.length) +
                    formatted

        updateDisplay()
    }

    // =====================================================
    // ПОСЛЕДНЕЕ ЧИСЛО
    // =====================================================

    private fun getLastNumber(text: String): String {

        var index = text.length - 1

        while (index >= 0) {

            val char = text[index]

            if (
                char == '+' ||
                char == '-' ||
                char == '×' ||
                char == '÷'
            ) {

                break
            }

            index--
        }

        return text.substring(index + 1)
    }

    // =====================================================
    // УДАЛЕНИЕ
    // =====================================================

    private fun deleteLast() {

        if (expression.isEmpty()) {

            return
        }

        expression =
            expression.dropLast(1)

        if (expression.isEmpty()) {

            expression = "0"
        }

        updateDisplay()
    }

    // =====================================================
    // ОЧИСТКА
    // =====================================================

    private fun clearCalculator() {

        expression = ""

        shouldStartNewNumber = true

        calculatorDisplay.text = "0"
    }

    // =====================================================
    // ПОКАЗ НА ЭКРАНЕ
    // =====================================================

    private fun updateDisplay() {

        if (expression.isEmpty()) {

            calculatorDisplay.text = "0"

        } else {

            calculatorDisplay.text = expression
        }
    }

    // =====================================================
    // ВЫЧИСЛЕНИЕ ВСЕГО ВЫРАЖЕНИЯ
    // =====================================================

    private fun calculateExpression() {

        if (expression.isEmpty()) {

            return
        }

        // Если выражение заканчивается оператором,
        // удаляем его
        var cleanExpression =
            expression

        while (
            cleanExpression.endsWith("+") ||
            cleanExpression.endsWith("-") ||
            cleanExpression.endsWith("×") ||
            cleanExpression.endsWith("÷")
        ) {

            cleanExpression =
                cleanExpression.dropLast(1)
        }

        if (cleanExpression.isEmpty()) {

            return
        }

        try {

            val answer =
                evaluateExpression(cleanExpression)

            val formattedAnswer =
                formatNumber(answer)

            calculatorDisplay.text =
                "$cleanExpression = $formattedAnswer"

            // После "=" след
