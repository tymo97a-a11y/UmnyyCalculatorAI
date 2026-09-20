package com.smartcalculator.ai

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var calculatorDisplay: TextView
    private lateinit var history: TextView
    private lateinit var calculateButton: Button
    private lateinit var photoButton: Button
    private lateinit var photoStatus: TextView

    private val client = OkHttpClient()

    private val serverUrl =
        "https://umnyy-calculator-ai-server.onrender.com/v1/calculate"

    private val historyList = mutableListOf<String>()

    private var selectedPhotoUri: Uri? = null

    /*
     * Выбор фотографии из галереи.
     */
    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                selectedPhotoUri = uri

                photoStatus.visibility = TextView.VISIBLE

                photoStatus.text =
                    "📷 Фото выбрано.\n" +
                    "Следующим шагом AI распознает задачу на изображении."

                result.text =
                    "📷 Фото готово к обработке AI.\n\n" +
                    "Функция распознавания будет подключена следующим шагом."

                Toast.makeText(
                    this,
                    "Фото выбрано",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        input = findViewById(R.id.input)
        result = findViewById(R.id.result)
        calculatorDisplay = findViewById(R.id.calculatorDisplay)
        history = findViewById(R.id.history)
        calculateButton = findViewById(R.id.calculateButton)
        photoButton = findViewById(R.id.photoButton)
        photoStatus = findViewById(R.id.photoStatus)

        setupCalculator()
        setupAI()
        setupPhoto()
        loadHistory()
    }

    // =========================================================
    // AI
    // =========================================================

    private fun setupAI() {

        calculateButton.setOnClickListener {

            val text =
                input.text.toString().trim()

            if (text.isEmpty()) {

                Toast.makeText(
                    this,
                    "Введите задачу",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            sendToAI(text)
        }
    }

    private fun sendToAI(text: String) {

        calculateButton.isEnabled = false
        calculateButton.text = "Считаю..."

        result.text =
            "🤖 AI анализирует задачу..."

        val json = JSONObject()

        json.put(
            "text",
            text
        )

        val body =
            json.toString()
                .toRequestBody(
                    "application/json; charset=utf-8"
                        .toMediaType()
                )

        val request =
            Request.Builder()
                .url(serverUrl)
                .post(body)
                .addHeader(
                    "Content-Type",
                    "application/json"
                )
                .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        calculateButton.isEnabled =
                            true

                        calculateButton.text =
                            "Рассчитать с AI"

                        result.text =
                            "❌ Не удалось подключиться к AI.\n\n" +
                            "Проверь интернет-соединение и попробуй ещё раз."

                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка подключения к серверу",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: okhttp3.Response
                ) {

                    response.use {

                        val responseText =
                            it.body?.string().orEmpty()

                        if (!it.isSuccessful) {

                            runOnUiThread {

                                calculateButton.isEnabled =
                                    true

                                calculateButton.text =
                                    "Рассчитать с AI"

                                result.text =
                                    "❌ Сервер вернул ошибку.\n\n" +
                                    "Код: ${it.code}"
                            }

                            return
                        }

                        try {

                            val jsonResponse =
                                JSONObject(responseText)

                            val aiResult =
                                jsonResponse.optString(
                                    "result",
                                    "Результат не получен"
                                )

                            val explanation =
                                jsonResponse.optString(
                                    "explanation",
                                    ""
                                )

                            val finalText =
                                buildString {

                                    append(
                                        "ИТОГ:\n"
                                    )

                                    append(aiResult)

                                    if (
                                        explanation.isNotBlank()
                                    ) {

                                        append(
                                            "\n\n"
                                        )

                                        append(
                                            "ОБЪЯСНЕНИЕ:\n"
                                        )

                                        append(
                                            explanation
                                        )
                                    }
                                }

                            runOnUiThread {

                                calculateButton.isEnabled =
                                    true

                                calculateButton.text =
                                    "Рассчитать с AI"

                                result.text =
                                    finalText

                                addHistory(
                                    "🤖 $text\n→ $aiResult"
                                )
                            }

                        } catch (e: Exception) {

                            runOnUiThread {

                                calculateButton.isEnabled =
                                    true

                                calculateButton.text =
                                    "Рассчитать с AI"

                                result.text =
                                    "❌ Не удалось обработать ответ AI."
                            }
                        }
                    }
                }
            })
    }

    // =========================================================
    // ФОТО
    // =========================================================

    private fun setupPhoto() {

        photoButton.setOnClickListener {

            photoPicker.launch("image/*")
        }
    }

    // =========================================================
    // ОБЫЧНЫЙ КАЛЬКУЛЯТОР
    // =========================================================

    private fun setupCalculator() {

        val numbers =
            mapOf(
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

        for ((id, value) in numbers) {

            findViewById<Button>(id)
                .setOnClickListener {

                    addToExpression(value)
                }
        }

        findViewById<Button>(
            R.id.buttonDot
        ).setOnClickListener {

            addToExpression(".")
        }

        findViewById<Button>(
            R.id.buttonPlus
        ).setOnClickListener {

            addOperator("+")
        }

        findViewById<Button>(
            R.id.buttonMinus
        ).setOnClickListener {

            addOperator("-")
        }

        findViewById<Button>(
            R.id.buttonMultiply
        ).setOnClickListener {

            addOperator("*")
        }

        findViewById<Button>(
            R.id.buttonDivide
        ).setOnClickListener {

            addOperator("/")
        }

        findViewById<Button>(
            R.id.buttonOpenBracket
        ).setOnClickListener {

            addOpenBracket()
        }

        findViewById<Button>(
            R.id.buttonCloseBracket
        ).setOnClickListener {

            addCloseBracket()
        }

        findViewById<Button>(
            R.id.buttonPercent
        ).setOnClickListener {

            addPercent()
        }

        findViewById<Button>(
            R.id.buttonBackspace
        ).setOnClickListener {

            backspace()
        }

        findViewById<Button>(
            R.id.buttonClearCalc
        ).setOnClickListener {

            clearCalculator()
        }

        findViewById<Button>(
            R.id.buttonEquals
        ).setOnClickListener {

            calculateExpression()
        }

        findViewById<Button>(
            R.id.clearButton
        ).setOnClickListener {

            clearHistory()
        }
    }

    private fun addToExpression(
        value: String
    ) {

        val current =
            calculatorDisplay.text.toString()

        if (
            current == "0" &&
            value != "."
        ) {

            calculatorDisplay.text =
                value

        } else {

            calculatorDisplay.append(
                value
            )
        }
    }

    private fun addOperator(
        operator: String
    ) {

        val current =
            calculatorDisplay.text.toString()

        if (
            current.isEmpty() ||
            current == "0"
        ) {

            if (operator == "-") {

                calculatorDisplay.text =
                    "-"
            }

            return
        }

        val last =
            current.last()

        if (
            last == '+' ||
            last == '-' ||
            last == '*' ||
            last == '/'
        ) {

            calculatorDisplay.text =
                current.dropLast(1) +
                        operator

        } else {

            calculatorDisplay.append(
                operator
            )
        }
    }

    private fun addOpenBracket() {

        val current =
            calculatorDisplay.text.toString()

        if (current == "0") {

            calculatorDisplay.text =
                "("

        } else {

            val last =
                current.last()

            if (
                last.isDigit() ||
                last == ')'
            ) {

                calculatorDisplay.append(
                    "*("
                )

            } else {

                calculatorDisplay.append(
                    "("
                )
            }
        }
    }

    private fun addCloseBracket() {

        val current =
            calculatorDisplay.text.toString()

        if (current.isEmpty()) {
            return
        }

        val openCount =
            current.count {
                it == '('
            }

        val closeCount =
            current.count {
                it == ')'
            }

        if (
            openCount <= closeCount
        ) {
            return
        }

        val last =
            current.last()

        if (
            last.isDigit() ||
            last == ')'
        ) {

            calculatorDisplay.append(
                ")"
            )
        }
    }

    private fun addPercent() {

        val current =
            calculatorDisplay.text.toString()

        if (current.isNotEmpty()) {

            val last =
                current.last()

            if (
                last.isDigit() ||
                last == ')'
            ) {

                calculatorDisplay.append(
                    "%"
                )
            }
        }
    }

    private fun backspace() {

        val current =
            calculatorDisplay.text.toString()

        if (current.length <= 1) {

            calculatorDisplay.text =
                "0"

        } else {

            calculatorDisplay.text =
                current.dropLast(1)
        }
    }

    private fun clearCalculator() {

        calculatorDisplay.text =
            "0"
    }

    // =========================================================
    // ВЫЧИСЛЕНИЕ
    // =========================================================

    private fun calculateExpression() {

        val expression =
            calculatorDisplay.text
                .toString()
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace(",", ".")

        if (
            expression.isBlank() ||
            expression == "0"
        ) {
            return
        }

        try {

            val value =
                evaluateExpression(
                    expression
                )

            val formatted =
                formatNumber(value)

            calculatorDisplay.text =
                formatted

            addHistory(
                "$expression = $formatted"
            )

        } catch (e: Exception) {

            calculatorDisplay.text =
                "Ошибка"

            Toast.makeText(
                this,
                "Проверь выражение",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun evaluateExpression(
        expression: String
    ): Double {

        val parser =
            ExpressionParser(
                expression
            )

        return parser.parse()
    }

    // =========================================================
    // ФОРМАТ ЧИСЛА
    // =========================================================

    private fun formatNumber(
        number: Double
    ): String {

        if (
            number.isNaN() ||
            number.isInfinite()
        ) {

            return "Ошибка"
        }

        if (
            number ==
            number.toLong().toDouble()
        ) {

            return number
                .toLong()
                .toString()
        }

        return String.format(
            Locale.US,
            "%.10f",
            number
        )
            .trimEnd('0')
            .trimEnd('.')
    }

    // =========================================================
    // ИСТОРИЯ
    // =========================================================

    private fun addHistory(
        text: String
    ) {

        historyList.add(
            0,
            text
        )

        if (
            historyList.size > 30
        ) {

            historyList.removeAt(
                historyList.lastIndex
            )
        }

        saveHistory()
        showHistory()
    }

    private fun showHistory() {

        if (
            historyList.isEmpty()
        ) {

            history.text =
                "История расчётов пока пуста"

            return
        }

        history.text =
            historyList.joinToString(
                "\n\n"
            )
    }

    private fun saveHistory() {

        val preferences =
            getSharedPreferences(
                "calculator_history",
                MODE_PRIVATE
            )

        preferences.edit()
            .putString(
                "history",
                historyList.joinToString(
                    "\n|||HISTORY|||"
                )
            )
            .apply()
    }

    private fun loadHistory() {

        val preferences =
            getSharedPreferences(
                "calculator_history",
                MODE_PRIVATE
            )

        val saved =
            preferences.getString(
                "history",
                ""
            ).orEmpty()

        if (saved.isNotEmpty()) {

            historyList.clear()

            historyList.addAll(
                saved.split(
                    "\n|||HISTORY|||"
                )
            )
        }

        showHistory()
    }

    private fun clearHistory() {

        historyList.clear()

        val preferences =
            getSharedPreferences(
                "calculator_history",
                MODE_PRIVATE
            )

        preferences.edit()
            .remove("history")
            .apply()

        showHistory()
    }

    // =========================================================
    // ПАРСЕР
    // =========================================================

    private class ExpressionParser(
        private val expression: String
    ) {

        private var position = 0

        fun parse(): Double {

            val result =
                parseExpression()

            skipSpaces()

            if (
                position <
                expression.length
            ) {

                throw IllegalArgumentException(
                    "Неожиданный символ"
                )
            }

            return result
        }

        private fun parseExpression(): Double {

            var result =
                parseTerm()

            while (true) {

                skipSpaces()

                if (match('+')) {

                    result +=
                        parseTerm()

                } else if (
                    match('-')
                ) {

                    result -=
                        parseTerm()

                } else {

                    return result
                }
            }
        }

        private fun parseTerm(): Double {

            var result =
                parseFactor()

            while (true) {

                skipSpaces()

                if (match('*')) {

                    result *=
                        parseFactor()

                } else if (
                    match('/')
                ) {

                    val divisor =
                        parseFactor()

                    if (
                        divisor == 0.0
                    ) {

                        throw ArithmeticException(
                            "Деление на ноль"
                        )
                    }

                    result /= divisor

                } else {

                    return result
                }
            }
        }

        private fun parseFactor(): Double {

            skipSpaces()

            if (match('+')) {
                return parseFactor()
            }

            if (match('-')) {
                return -parseFactor()
            }

            val result: Double

            if (match('(')) {

                result =
                    parseExpression()

                if (!match(')')) {

                    throw IllegalArgumentException(
                        "Не закрыта скобка"
                    )
                }

            } else {

                result =
                    parseNumber()
            }

            skipSpaces()

            var finalResult =
                result

            while (match('%')) {

                finalResult /=
                    100.0

                skipSpaces()
            }

            return finalResult
        }

        private fun parseNumber(): Double {

            skipSpaces()

            val start =
                position

            var hasDot =
                false

            while (
                position <
                expression.length
            ) {

                val character =
                    expression[position]

                if (
                    character.isDigit()
                ) {

                    position++

                } else if (
                    character == '.' &&
                    !hasDot
                ) {

                    hasDot = true
                    position++

                } else {

                    break
                }
            }

            if (
                start == position
            ) {

                throw IllegalArgumentException(
                    "Ожидалось число"
                )
            }

            return expression
                .substring(
                    start,
                    position
                )
                .toDouble()
        }

        private fun match(
            character: Char
        ): Boolean {

            skipSpaces()

            if (
                position <
                expression.length &&
                expression[position] ==
                character
            ) {

                position++

                return true
            }

            return false
        }

        private fun skipSpaces() {

            while (
                position <
                expression.length &&
                expression[position]
                    .isWhitespace()
            ) {

                position++
            }
        }
    }
}
