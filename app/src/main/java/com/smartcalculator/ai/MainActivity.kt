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

    private var expression = ""
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

        loadHistory()
        setupCalculatorButtons()

        val calculateButton =
            findViewById<Button>(R.id.calculateButton)

        val clearButton =
            findViewById<Button>(R.id.clearButton)

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

        findViewById<Button>(R.id.buttonOpenBracket)
            .setOnClickListener {
                addOpenBracket()
            }

        findViewById<Button>(R.id.buttonCloseBracket)
            .setOnClickListener {
                addCloseBracket()
            }
    }

    // =====================================================
    // ЧИСЛА
    // =====================================================

    private fun addNumber(number: String) {

        if (shouldStartNewNumber) {

            expression = number
            shouldStartNewNumber = false

        } else {

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

        val lastNumber = getLastNumber(expression)

        if (lastNumber.contains(".")) {
            return
        }

        if (
            expression.isEmpty() ||
            expression.endsWith("+") ||
            expression.endsWith("-") ||
            expression.endsWith("×") ||
            expression.endsWith("÷") ||
            expression.endsWith("(")
        ) {

            expression += "0."

        } else {

            expression += "."
        }

        updateDisplay()
    }

    // =====================================================
    // ПЛЮС / МИНУС / УМНОЖЕНИЕ / ДЕЛЕНИЕ
    // =====================================================

    private fun addOperator(operator: String) {

        if (expression.isEmpty()) {
            return
        }

        if (
            expression.endsWith("+") ||
            expression.endsWith("-") ||
            expression.endsWith("×") ||
            expression.endsWith("÷")
        ) {

            expression =
                expression.dropLast(1) + operator

        } else if (expression.endsWith("(")) {

            return

        } else {

            expression += operator
        }

        shouldStartNewNumber = false

        updateDisplay()
    }

    // =====================================================
    // ОТКРЫВАЮЩАЯ СКОБКА
    // =====================================================

    private fun addOpenBracket() {

        if (shouldStartNewNumber) {

            expression = "("
            shouldStartNewNumber = false

        } else if (
            expression.isEmpty() ||
            expression.endsWith("+") ||
            expression.endsWith("-") ||
            expression.endsWith("×") ||
            expression.endsWith("÷") ||
            expression.endsWith("(")
        ) {

            expression += "("

        } else {

            // Например 2(3+4)
            // автоматически превращаем в 2×(3+4)
            expression += "×("
        }

        updateDisplay()
    }

    // =====================================================
    // ЗАКРЫВАЮЩАЯ СКОБКА
    // =====================================================

    private fun addCloseBracket() {

        if (expression.isEmpty()) {
            return
        }

        val openCount =
            expression.count { it == '(' }

        val closeCount =
            expression.count { it == ')' }

        if (openCount <= closeCount) {
            return
        }

        if (
            expression.endsWith("+") ||
            expression.endsWith("-") ||
            expression.endsWith("×") ||
            expression.endsWith("÷") ||
            expression.endsWith("(")
        ) {
            return
        }

        expression += ")"

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

        expression =
            expression.dropLast(lastNumber.length) +
                    formatNumber(percent)

        updateDisplay()
    }

    // =====================================================
    // ПОЛУЧЕНИЕ ПОСЛЕДНЕГО ЧИСЛА
    // =====================================================

    private fun getLastNumber(text: String): String {

        var index = text.length - 1

        while (index >= 0) {

            val char = text[index]

            if (
                char == '+' ||
                char == '-' ||
                char == '×' ||
                char == '÷' ||
                char == '(' ||
                char == ')'
            ) {
                break
            }

            index--
        }

        return text.substring(index + 1)
    }

    // =====================================================
    // BACKSPACE
    // =====================================================

    private fun deleteLast() {

        if (expression.isEmpty()) {
            return
        }

        expression =
            expression.dropLast(1)

        if (expression.isEmpty()) {

            expression = ""
            shouldStartNewNumber = true
        }

        updateDisplay()
    }

    // =====================================================
    // AC
    // =====================================================

    private fun clearCalculator() {

        expression = ""

        shouldStartNewNumber = true

        calculatorDisplay.text = "0"
    }

    // =====================================================
    // ЭКРАН
    // =====================================================

    private fun updateDisplay() {

        calculatorDisplay.text =
            if (expression.isEmpty()) {
                "0"
            } else {
                expression
            }
    }

    // =====================================================
    // ВЫЧИСЛЕНИЕ
    // =====================================================

    private fun calculateExpression() {

        if (expression.isEmpty()) {
            return
        }

        var cleanExpression = expression

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

        val openCount =
            cleanExpression.count { it == '(' }

        val closeCount =
            cleanExpression.count { it == ')' }

        if (openCount != closeCount) {

            calculatorDisplay.text =
                "Ошибка: проверьте скобки"

            return
        }

        try {

            val answer =
                evaluateExpression(cleanExpression)

            val formattedAnswer =
                formatNumber(answer)

            calculatorDisplay.text =
                "$cleanExpression = $formattedAnswer"

            expression = formattedAnswer

            shouldStartNewNumber = true

        } catch (e: ArithmeticException) {

            calculatorDisplay.text =
                "Ошибка: деление на 0"

            shouldStartNewNumber = true

        } catch (e: Exception) {

            calculatorDisplay.text =
                "Ошибка"

            shouldStartNewNumber = true
        }
    }

    // =====================================================
    // МАТЕМАТИЧЕСКИЙ ПАРСЕР
    // =====================================================

    private class ExpressionParser(
        private val text: String
    ) {

        private var position = 0

        fun parse(): Double {

            val result =
                parseExpression()

            skipSpaces()

            if (position != text.length) {
                throw IllegalArgumentException()
            }

            return result
        }

        // + и -
        private fun parseExpression(): Double {

            var value =
                parseTerm()

            while (true) {

                skipSpaces()

                if (match('+')) {

                    value += parseTerm()

                } else if (match('-')) {

                    value -= parseTerm()

                } else {

                    break
                }
            }

            return value
        }

        // × и ÷
        private fun parseTerm(): Double {

            var value =
                parseFactor()

            while (true) {

                skipSpaces()

                if (match('×')) {

                    value *= parseFactor()

                } else if (match('÷')) {

                    val divisor =
                        parseFactor()

                    if (divisor == 0.0) {
                        throw ArithmeticException()
                    }

                    value /= divisor

                } else {

                    break
                }
            }

            return value
        }

        // Отрицательные числа
        private fun parseFactor(): Double {

            skipSpaces()

            if (match('+')) {
                return parseFactor()
            }

            if (match('-')) {
                return -parseFactor()
            }

            return parsePrimary()
        }

        // Число или скобки
        private fun parsePrimary(): Double {

            skipSpaces()

            if (match('(')) {

                val value =
                    parseExpression()

                skipSpaces()

                if (!match(')')) {
                    throw IllegalArgumentException()
                }

                return value
            }

            return parseNumber()
        }

        private fun parseNumber(): Double {

            skipSpaces()

            val start =
                position

            var hasDot = false

            while (position < text.length) {

                val char =
                    text[position]

                if (char.isDigit()) {

                    position++

                } else if (char == '.' && !hasDot) {

                    hasDot = true
                    position++

                } else {

                    break
                }
            }

            if (start == position) {
                throw IllegalArgumentException()
            }

            return text
                .substring(start, position)
                .toDouble()
        }

        private fun match(char: Char): Boolean {

            if (
                position < text.length &&
                text[position] == char
            ) {

                position++

                return true
            }

            return false
        }

        private fun skipSpaces() {

            while (
                position < text.length &&
                text[position].isWhitespace()
            ) {

                position++
            }
        }
    }

    // =====================================================
    // ЗАПУСК ПАРСЕРА
    // =====================================================

    private fun evaluateExpression(
        expressionText: String
    ): Double {

        return ExpressionParser(
            expressionText
        ).parse()
    }

    // =====================================================
    // ФОРМАТ ЧИСЛА
    // =====================================================

    private fun formatNumber(
        number: Double
    ): String {

        if (
            number.isFinite() &&
            number == number.toLong().toDouble()
        ) {

            return number.toLong().toString()
        }

        return String.format(
            Locale.US,
            "%.8f",
            number
        )
            .trimEnd('0')
            .trimEnd('.')
    }

    // =====================================================
    // AI
    // =====================================================

    private fun sendToAI(text: String) {

        val json =
            JSONObject()

        json.put(
            "text",
            text
        )

        val mediaType =
            "application/json; charset=utf-8"
                .toMediaType()

        val body =
            json.toString()
                .toRequestBody(mediaType)

        val request =
            Request.Builder()
                .url(
                    "https://umnyy-calculator-ai-server.onrender.com/v1/calculate"
                )
                .post(body)
                .addHeader(
                    "Content-Type",
                    "application/json"
                )
                .build()

        client.newCall(request)
            .enqueue(
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
                                    JSONObject(
                                        responseText ?: "{}"
                                    )

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
                                    if (
                                        explanation.isNotEmpty()
                                    ) {

                                        "$answer\n\n$explanation"

                                    } else {

                                        answer
                                    }

                                result.text =
                                    finalAnswer

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

    // =====================================================
    // ИСТОРИЯ
    // =====================================================

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

        history.text =
            limitedHistory
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
